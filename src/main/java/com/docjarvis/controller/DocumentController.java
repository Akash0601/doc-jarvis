package com.docjarvis.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docjarvis.document.Document;
import com.docjarvis.entity.User;
import com.docjarvis.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {

        Document saved = documentService.uploadDocument(file, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Document>> getUserDocuments(
            @AuthenticationPrincipal User user) {

        List<Document> documents = documentService.getUserDocuments(user);
        return ResponseEntity.ok(documents);
    }
}