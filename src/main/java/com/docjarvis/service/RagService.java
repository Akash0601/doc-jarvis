package com.docjarvis.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ChatLanguageModel chatLanguageModel;

    public record RagResult(String answer, String sourceDocument, Integer pageNumber) {}

    public RagResult generateAnswer(String question, 
                                    List<EmbeddingMatch<TextSegment>> relevantChunks,
                                    String documentName) {

        log.info("Generating RAG answer for question: '{}'", question);

        // Step 1: Build context string from retrieved chunks
        if (relevantChunks.isEmpty()) {
            log.warn("No relevant chunks found for question: '{}'", question);
            return new RagResult(
                "I could not find any relevant information in the document to answer your question.",
                documentName,
                null
            );
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < relevantChunks.size(); i++) {
            EmbeddingMatch<TextSegment> match = relevantChunks.get(i);
            if (match.embedded() != null) {
                contextBuilder.append("--- Chunk ").append(i + 1)
                        .append(" (relevance score: ")
                        .append(String.format("%.2f", match.score()))
                        .append(") ---\n")
                        .append(match.embedded().text())
                        .append("\n\n");
            }
        }
        String context = contextBuilder.toString();

        // Step 2: Build system prompt — instructs LLM to stay grounded
        String systemPrompt = """
                You are a document assistant. Your job is to answer questions
                strictly based on the document context provided below.
                
                Rules:
                1. Only use information from the provided context to answer.
                2. If the answer is not in the context, say exactly:
                   "I cannot find the answer to this question in the provided document."
                3. Do not use any outside knowledge or make assumptions.
                4. Keep your answer concise and directly relevant to the question.
                5. Do not mention "chunks" or "context" in your answer — 
                   respond as if you are summarizing the document directly.
                
                Document context:
                """ + context;

        // Step 3: Build user message — the actual question
        String userPrompt = "Question: " + question;

        log.info("Sending prompt to LLM with {} chunks as context", relevantChunks.size());

        // Step 4: Call Ollama via LangChain4j
        dev.langchain4j.model.chat.response.ChatResponse chatResponse = chatLanguageModel.chat(
            List.of(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(userPrompt)
            )
        );
        String answer = chatResponse.aiMessage().text();

        log.info("LLM response received, length: {} characters", answer.length());

        // Step 5: Extract citation from the highest-scoring chunk
        Integer pageNumber = null;
        EmbeddingMatch<TextSegment> topMatch = relevantChunks.get(0);
        if (topMatch.embedded() != null) {
            String pageStr = topMatch.embedded().metadata().getString("pageNumber");
            if (pageStr != null) {
                try {
                    pageNumber = Integer.parseInt(pageStr);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse pageNumber from metadata: {}", pageStr);
                }
            }
        }

        return new RagResult(answer, documentName, pageNumber);
    }
}