package com.docjarvis.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.docjarvis.dto.FlashcardCard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;

@Service
public class FlashcardService {

    private final SemanticSearchService semanticSearchService;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public FlashcardService(SemanticSearchService semanticSearchService,
                            ChatLanguageModel chatLanguageModel,
                            ObjectMapper objectMapper) {
        this.semanticSearchService = semanticSearchService;
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
    }

    public List<FlashcardCard> generateFlashcards(Long documentId) {

        // 1. Get relevant chunks from Qdrant (returns EmbeddingMatch objects)
        List<EmbeddingMatch<TextSegment>> matches =
                semanticSearchService.findRelevantChunks("key concepts definitions important facts", documentId, 100, 0.0);

        // 2. Extract the raw text from each match
        String context = matches.stream()
                .filter(m -> m.embedded() != null)
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));

        // 3. Build a strict JSON-only prompt
        String prompt = """
                You are a flashcard generator. Based ONLY on the document content below,
                generate exactly 5 flashcard question-answer pairs.

                RULES:
                - Return ONLY a valid JSON array. No explanation, no markdown, no extra text.
                - Each object must have exactly two fields: "question" and "answer".
                - Questions should test understanding of key concepts.
                - Answers should be concise (1-3 sentences).

                REQUIRED OUTPUT FORMAT:
                [
                  {"question": "...", "answer": "..."},
                  {"question": "...", "answer": "..."},
                  {"question": "...", "answer": "..."},
                  {"question": "...", "answer": "..."},
                  {"question": "...", "answer": "..."}
                ]

                DOCUMENT CONTENT:
                %s
                """.formatted(context);

        // 4. Call the LLM
        String rawResponse = chatLanguageModel.chat(prompt);

        // 5. Strip any accidental markdown fences and parse JSON
        String cleaned = rawResponse
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .replaceAll("<EOL>", "\n")
                .trim();

        // Fix truncated JSON — find last complete object and close the array
        if (!cleaned.endsWith("]")) {
            int lastBrace = cleaned.lastIndexOf("}");
            if (lastBrace != -1) {
                cleaned = cleaned.substring(0, lastBrace + 1) + "\n]";
            }
        }

        try {
            return objectMapper.readValue(cleaned, new TypeReference<List<FlashcardCard>>() {});
        } catch (Exception e) {
            // Log the actual Jackson error message
            throw new RuntimeException("Failed to parse flashcard JSON. Jackson error: " 
                + e.getMessage() + " | Raw cleaned string length: " 
                + cleaned.length() + " | First 50 chars: " 
                + cleaned.substring(0, Math.min(50, cleaned.length())), e);
        }
    }
}