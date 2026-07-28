package com.docjarvis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@Configuration
public class EmbeddingConfig {

    @Bean
    @Primary
    @Lazy
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    // ─── LOCAL PROFILE ──────────────────────────────────────────────────────

    @Bean
    @Profile("local")
    public QdrantEmbeddingStore localQdrantEmbeddingStore(
            @Value("${langchain4j.qdrant.host}") String host,
            @Value("${langchain4j.qdrant.port}") int port) {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName("documents")
                .build();
    }

    @Bean
    @Profile("local")
    public QdrantClient localQdrantClient(
            @Value("${langchain4j.qdrant.host}") String host,
            @Value("${langchain4j.qdrant.port}") int port) {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build()
        );
    }

    // ─── PROD PROFILE ───────────────────────────────────────────────────────

    @Bean
    @Profile("prod")
    @Lazy
    public QdrantEmbeddingStore prodQdrantEmbeddingStore(
            @Value("${langchain4j.qdrant.host}") String host,
            @Value("${qdrant.cloud.api-key}") String apiKey) {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .apiKey(apiKey)
                .collectionName("documents")
                .useTls(true)
                .port(6334)
                .build();
    }

    @Bean
    @Profile("prod")
    @Lazy
    public QdrantClient cloudQdrantClient(
            @Value("${langchain4j.qdrant.host}") String host,
            @Value("${qdrant.cloud.api-key}") String apiKey) {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(host, 6334, true)
                        .withApiKey(apiKey)
                        .build()
        );
    }
}