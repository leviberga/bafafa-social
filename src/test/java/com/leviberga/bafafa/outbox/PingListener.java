package com.leviberga.bafafa.outbox;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
class PingListener {

    final List<String> received = new CopyOnWriteArrayList<>();
    final Set<String> failOnce = ConcurrentHashMap.newKeySet();

    @EventListener
    void on(TestPing ping) {
        if (failOnce.remove(ping.value())) {
            throw new IllegalStateException("falha simulada");
        }
        received.add(ping.value());
    }
}