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

    public List<EmbeddingMatch<TextSegment>> findRelevantChunks(String query, Long documentId, int maxResults, double minScore) {

        log.info("Searching for relevant chunks for query: '{}', documentId: {}", query, documentId);

        TextSegment querySegment = TextSegment.from(query);
        Embedding queryEmbedding = embeddingModel.embed(querySegment).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult =
                qdrantEmbeddingStore.search(searchRequest);

        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
        log.info("Found {} relevant chunks", matches.size());

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

    public List<EmbeddingMatch<TextSegment>> findRelevantChunks(String query, Long documentId, int maxResults) {
        return findRelevantChunks(query, documentId, maxResults, 0.3);
    }
}