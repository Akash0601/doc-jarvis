package com.docjarvis.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmbeddingService {

    private static final String COLLECTION_NAME = "documents";
    private static final int VECTOR_DIMENSION = 384;

    private final EmbeddingModel embeddingModel;
    private final TextChunkingService textChunkingService;
    private final QdrantEmbeddingStore embeddingStore;

    @Value("${langchain4j.qdrant.host}")
    private String qdrantHost;

    @Value("${langchain4j.qdrant.port}")
    private int qdrantPort;

    public EmbeddingService(EmbeddingModel embeddingModel,
                            TextChunkingService textChunkingService,
                            QdrantEmbeddingStore embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.textChunkingService = textChunkingService;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void initQdrant() throws ExecutionException, InterruptedException {
        QdrantClient qdrantClient = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );

        boolean collectionExists = qdrantClient.listCollectionsAsync()
                .get()
                .stream()
                .anyMatch(c -> c.equals(COLLECTION_NAME));

        if (!collectionExists) {
            log.info("Creating Qdrant collection: {}", COLLECTION_NAME);
            qdrantClient.createCollectionAsync(
                COLLECTION_NAME,
                VectorParams.newBuilder()
                    .setSize(VECTOR_DIMENSION)
                    .setDistance(Distance.Cosine)
                    .build()
            ).get();
            log.info("Qdrant collection '{}' created successfully", COLLECTION_NAME);
        } else {
            log.info("Qdrant collection '{}' already exists", COLLECTION_NAME);
        }
    }

    public void embedAndStore(Long documentId, String fullText) {
        log.info("Starting embedding pipeline for documentId: {}", documentId);

        List<String> chunks = textChunkingService.chunkText(fullText);
        log.info("Document {} split into {} chunks", documentId, chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            Metadata metadata = new Metadata();
            metadata.put("documentId", documentId.toString());
            metadata.put("chunkIndex", String.valueOf(i));
            metadata.put("chunkText", chunkText);

            TextSegment segment = TextSegment.from(chunkText, metadata);

            Response<Embedding> response = embeddingModel.embed(segment);
            Embedding embedding = response.content();

            embeddingStore.add(embedding, segment);

            log.info("Stored chunk {}/{} for documentId {}", i + 1, chunks.size(), documentId);
        }

        log.info("Embedding pipeline complete for documentId: {}. Total chunks stored: {}", 
                 documentId, chunks.size());
    }
}