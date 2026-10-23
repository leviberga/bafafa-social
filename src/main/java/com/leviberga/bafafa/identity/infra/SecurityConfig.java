package com.leviberga.bafafa.identity.infra;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF desligado: a API é stateless e o cookie de refresh é SameSite=Strict, restrito a /api/v1/auth.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/profiles/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/profiles/*/follow-stats",
                                "/api/v1/timeline/users/*").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${bafafa.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true); // necessário para o cookie de refresh
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties props) {
        return NimbusJwtDecoder.withSecretKey(secretKey(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey secretKey(AuthProperties props) {
        byte[] bytes = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("bafafa.auth.jwt-secret precisa ter pelo menos 32 bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}