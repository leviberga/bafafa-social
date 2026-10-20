package com.leviberga.bafafa.engagement.domain;

public interface PostStatsOperations {

    // Soma 1 ao contador, criando o documento se ainda não existir.
    void incrementLikes(String postId);

    // Subtrai 1, sem nunca deixar o contador abaixo de zero.
    void decrementLikes(String postId);
}