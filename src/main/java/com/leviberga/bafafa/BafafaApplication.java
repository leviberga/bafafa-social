package com.leviberga.bafafa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

@EnableAsync
@SpringBootApplication
public class BafafaApplication {

	public static void main(String[] args) {

		SpringApplication.run(BafafaApplication.class, args);


	}
	@Bean
    Clock clock() {
		return Clock.systemUTC();
	}

}
