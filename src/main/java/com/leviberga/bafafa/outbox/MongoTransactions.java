package com.leviberga.bafafa.outbox;

import com.mongodb.MongoException;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Executa trabalho dentro de uma transação do MongoDB, com retentativa para erros transitórios
 * (por exemplo, conflito de escrita entre transações simultâneas).
 * Não é um bean de PlatformTransactionManager de propósito: os @Transactional do JPA seguem intactos.
 */
@Component
public class MongoTransactions {

    private static final int MAX_ATTEMPTS = 5;

    private final TransactionTemplate template;

    public MongoTransactions(MongoDatabaseFactory factory) {
        this.template = new TransactionTemplate(new MongoTransactionManager(factory));
    }

    public <T> T execute(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return template.execute(status -> work.get());
            } catch (RuntimeException e) {
                if (attempt >= MAX_ATTEMPTS || !isTransient(e)) {
                    throw e;
                }
                pause(attempt);
            }
        }
    }

    public void run(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }

    private static boolean isTransient(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof MongoException mongo
                    && mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }

    private static void pause(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(5, 25) * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}