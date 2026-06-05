package com.docjarvis.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String COLLECTION_NAME = "documents";
    private static final int VECTOR_DIMENSION = 384; // Gemini text-embedding-004 dimension

    private final EmbeddingModel embeddingModel;
    private final TextChunkingService textChunkingService;
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${langchain4j.qdrant.host}")
    private String qdrantHost;

    @Value("${langchain4j.qdrant.port}")
    private int qdrantPort;

    public EmbeddingService(EmbeddingModel embeddingModel,
                            TextChunkingService textChunkingService) {
        this.embeddingModel = embeddingModel;
        this.textChunkingService = textChunkingService;
    }

    // Runs once after Spring initializes this bean
    @PostConstruct
    public void initQdrant() throws ExecutionException, InterruptedException {
        // Step 1: Create Qdrant gRPC client
        QdrantClient qdrantClient = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );

        // Step 2: Create collection if it doesn't already exist
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

        // Step 3: Build the LangChain4j embedding store pointing to our collection
        this.embeddingStore = QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName(COLLECTION_NAME)
                .build();
    }

    public void embedAndStore(Long documentId, String fullText) {
        log.info("Starting embedding pipeline for documentId: {}", documentId);

        // Step 4: Chunk the text
        List<String> chunks = textChunkingService.chunkText(fullText);
        log.info("Document {} split into {} chunks", documentId, chunks.size());

        // Step 5: Embed each chunk and store in Qdrant
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);

            // Build metadata — stored alongside the vector in Qdrant
            Metadata metadata = new Metadata();
            metadata.put("documentId", documentId.toString());
            metadata.put("chunkIndex", String.valueOf(i));
            metadata.put("chunkText", chunkText);

            TextSegment segment = TextSegment.from(chunkText, metadata);

            // Call Gemini API to get the embedding vector
            Response<Embedding> response = embeddingModel.embed(segment);
            Embedding embedding = response.content();

            // Store vector + metadata in Qdrant
            embeddingStore.add(embedding, segment);

            log.info("Stored chunk {}/{} for documentId {}", 
                     i + 1, chunks.size(), documentId);
        }

        log.info("Embedding pipeline complete for documentId: {}. " +
                 "Total chunks stored: {}", documentId, chunks.size());
    }
}