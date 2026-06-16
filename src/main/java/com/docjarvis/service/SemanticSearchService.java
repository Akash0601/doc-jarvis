package com.docjarvis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final EmbeddingModel embeddingModel;
    private final QdrantClient qdrantClient;

    private static final String COLLECTION_NAME = "documents";

    public List<EmbeddingMatch<TextSegment>> findRelevantChunks(
            String query, Long documentId, int maxResults) {
        return findRelevantChunks(query, documentId, maxResults, 0.0);
    }

    public List<EmbeddingMatch<TextSegment>> findRelevantChunks(
            String query, Long documentId, int maxResults, double minScore) {

        log.info("Searching for relevant chunks for query: '{}', documentId: {}", query, documentId);

        // 1. Embed the query
        TextSegment querySegment = TextSegment.from(query);
        Embedding queryEmbedding = embeddingModel.embed(querySegment).content();
        log.info("Query embedding dimension: {}", queryEmbedding.vector().length);

        // 2. Build vector list for gRPC
        List<Float> vector = new ArrayList<>();
        for (float v : queryEmbedding.vector()) {
            vector.add(v);
        }

        // 3. Search Qdrant directly via gRPC client
        List<ScoredPoint> scoredPoints;
        try {
            scoredPoints = qdrantClient.searchAsync(
                    SearchPoints.newBuilder()
                            .setCollectionName(COLLECTION_NAME)
                            .addAllVector(vector)
                            .setLimit(maxResults)
                            .setWithPayload(
                                io.qdrant.client.grpc.Points.WithPayloadSelector.newBuilder()
                                    .setEnable(true)
                                    .build()
                            )
                            .build()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Qdrant search failed", e);
            return List.of();
        }

        log.info("Found {} relevant chunks", scoredPoints.size());

        // 4. Filter by documentId and minScore, convert to EmbeddingMatch
        List<EmbeddingMatch<TextSegment>> filtered = new ArrayList<>();

        for (ScoredPoint point : scoredPoints) {
            double score = point.getScore();
            if (score < minScore) continue;

            // Extract payload fields
            var payloadMap = point.getPayloadMap();

            String docId = null;
            String chunkText = null;

            if (payloadMap.containsKey("documentId")) {
                docId = payloadMap.get("documentId").getStringValue();
            }
            if (payloadMap.containsKey("chunkText")) {
                chunkText = payloadMap.get("chunkText").getStringValue();
            }

            // Filter by documentId
            if (docId != null && !docId.equals(String.valueOf(documentId))) {
                continue;
            }

            // Build TextSegment with metadata
            dev.langchain4j.data.document.Metadata metadata =
                    new dev.langchain4j.data.document.Metadata();
            metadata.put("documentId", docId != null ? docId : "");

            if (payloadMap.containsKey("chunkIndex")) {
                metadata.put("chunkIndex",
                        payloadMap.get("chunkIndex").getStringValue());
            }

            TextSegment segment = TextSegment.from(
                    chunkText != null ? chunkText : "", metadata);

            filtered.add(new EmbeddingMatch<>(score, point.getId().toString(),
                    Embedding.from(new float[0]), segment));
        }

        log.info("After filtering by documentId {}: {} chunks remain",
                documentId, filtered.size());

        return filtered;
    }
}