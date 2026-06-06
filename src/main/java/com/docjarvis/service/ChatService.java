package com.docjarvis.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.docjarvis.chat.ChatMessage;
import com.docjarvis.chat.ChatMessageRepository;
import com.docjarvis.document.Document;
import com.docjarvis.dto.ChatRequest;
import com.docjarvis.dto.ChatResponse;
import com.docjarvis.entity.User;
import com.docjarvis.repository.DocumentRepository;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SemanticSearchService semanticSearchService;
    private final RagService ragService;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;

    public ChatResponse ask(ChatRequest request) {

        log.info("Processing chat request for documentId: {}", request.getDocumentId());

        // Step 1: Get the currently authenticated user from Spring Security context
        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        // Step 2: Load the document from PostgreSQL — verify it exists
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new RuntimeException(
                        "Document not found with id: " + request.getDocumentId()));

        // Step 3: Search Qdrant for relevant chunks
        List<EmbeddingMatch<TextSegment>> relevantChunks = semanticSearchService
                .findRelevantChunks(request.getQuestion(), request.getDocumentId());

        log.info("Found {} relevant chunks for question: '{}'",
                relevantChunks.size(), request.getQuestion());

        // Step 4: Generate grounded answer via RAG pipeline
        RagService.RagResult ragResult = ragService.generateAnswer(
                request.getQuestion(),
                relevantChunks,
                document.getFileName()
        );

        log.info("RAG answer generated, source: {}, page: {}",
                ragResult.sourceDocument(), ragResult.pageNumber());

        // Step 5: Persist the conversation to PostgreSQL
        LocalDateTime now = LocalDateTime.now();

        ChatMessage chatMessage = ChatMessage.builder()
                .question(request.getQuestion())
                .answer(ragResult.answer())
                .sourceDocument(ragResult.sourceDocument())
                .pageNumber(ragResult.pageNumber())
                .timestamp(now)
                .user(currentUser)
                .document(document)
                .build();

        chatMessageRepository.save(chatMessage);

        log.info("Chat message saved to PostgreSQL for documentId: {}",
                request.getDocumentId());

        // Step 6: Build and return the response DTO
        return ChatResponse.builder()
                .question(request.getQuestion())
                .answer(ragResult.answer())
                .sourceDocument(ragResult.sourceDocument())
                .pageNumber(ragResult.pageNumber())
                .timestamp(now)
                .build();
    }

    public List<ChatResponse> getChatHistory(Long documentId) {

        log.info("Fetching chat history for documentId: {}", documentId);

        return chatMessageRepository
                .findByDocumentIdOrderByTimestampAsc(documentId)
                .stream()
                .map(msg -> ChatResponse.builder()
                        .question(msg.getQuestion())
                        .answer(msg.getAnswer())
                        .sourceDocument(msg.getSourceDocument())
                        .pageNumber(msg.getPageNumber())
                        .timestamp(msg.getTimestamp())
                        .build())
                .toList();
    }
}