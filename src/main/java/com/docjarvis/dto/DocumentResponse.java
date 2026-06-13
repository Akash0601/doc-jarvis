package com.docjarvis.dto;

import java.time.LocalDateTime;

import com.docjarvis.document.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String fileName;
    private String fileType;
    private String extractedText;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private Long userId;
    private String userEmail;

    public static DocumentResponse fromDocument(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .extractedText(document.getExtractedText())
                .fileSize(document.getFileSize())
                .uploadedAt(document.getUploadedAt())
                .userId(document.getUser().getId())
                .userEmail(document.getUser().getEmail())
                .build();
    }
}
