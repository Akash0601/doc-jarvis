package com.docjarvis.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docjarvis.dto.ChatRequest;
import com.docjarvis.dto.ChatResponse;
import com.docjarvis.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        log.info("Received chat request for documentId: {}", request.getDocumentId());
        ChatResponse response = chatService.ask(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{documentId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable Long documentId) {
        log.info("Fetching chat history for documentId: {}", documentId);
        List<ChatResponse> history = chatService.getChatHistory(documentId);
        return ResponseEntity.ok(history);
    }
}