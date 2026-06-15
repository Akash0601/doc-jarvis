package com.docjarvis.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docjarvis.document.Document;
import com.docjarvis.dto.DocumentResponse;
import com.docjarvis.entity.User;
import com.docjarvis.repository.DocumentRepository;
import com.docjarvis.service.DocumentService;
import com.docjarvis.service.EmbeddingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        Document saved = documentService.uploadDocument(file, user);
        return ResponseEntity.ok(DocumentResponse.fromDocument(saved));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @AuthenticationPrincipal User user) {

        List<Document> documents = documentService.getUserDocuments(user);
        List<DocumentResponse> response = documents.stream()
                .map(DocumentResponse::fromDocument)
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        documentService.deleteDocument(documentId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/reembed")
    public ResponseEntity<String> reEmbed(@PathVariable Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        embeddingService.embedAndStore(documentId, document.getExtractedText());
        return ResponseEntity.ok("Re-embedding complete for documentId: " + documentId);
    }
}