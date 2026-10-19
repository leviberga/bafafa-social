package com.leviberga.bafafa;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.ThreadLocalRandom;

public final class TestClient {

    private TestClient() {
    }

    public static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    // Um IP aleatório (faixa 10.x.x.x). Cada chamada devolve um IP diferente.
    public static RequestPostProcessor newIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return ip("10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255)));
    }
}