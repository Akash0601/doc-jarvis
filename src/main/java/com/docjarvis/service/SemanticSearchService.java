package com.docjarvis.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final EmbeddingModel embeddingModel;
    private final QdrantEmbeddingStore qdrantEmbeddingStore;

    public List<EmbeddingMatch<TextSegment>> findRelevantChunks(String query, Long documentId) {

        log.info("Searching for relevant chunks for query: '{}', documentId: {}", query, documentId);

        // Step 1: Embed the user's query using the SAME model used during ingestion
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Step 2: Build the search request — top 5 most similar chunks
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.3)
                .build();

        // Step 3: Search Qdrant for similar vectors
        EmbeddingSearchResult<TextSegment> searchResult =
                qdrantEmbeddingStore.search(searchRequest);

        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        log.info("Found {} relevant chunks", matches.size());

        // Step 4: Filter by documentId — only return chunks from the requested document
        List<EmbeddingMatch<TextSegment>> filtered = matches.stream()
                .filter(match -> {
                    if (match.embedded() == null) return true;
                    String docId = match.embedded().metadata().getString("documentId");
                    return docId == null || docId.equals(String.valueOf(documentId));
                })
                .toList();

        log.info("After filtering by documentId {}: {} chunks remain", documentId, filtered.size());

        return filtered;
    }
}