package com.docjarvis.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docjarvis.document.Document;
import com.docjarvis.entity.User;
import com.docjarvis.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final Tika tika = new Tika();

    public Document uploadDocument(MultipartFile file, User user) throws IOException {

        // Step 1: Detect real MIME type
        String mimeType = tika.detect(file.getBytes());

        // Step 2: Validate file type
        if (!mimeType.equals("application/pdf") &&
            !mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are supported");
        }

        // Step 3: Extract text based on file type
        String extractedText;
        if (mimeType.equals("application/pdf")) {
            extractedText = extractFromPdf(file);
        } else {
            extractedText = extractFromDocx(file);
        }

        // Step 4: Clean the extracted text
        String cleanedText = cleanText(extractedText);

        // Step 5: Save to database
        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .fileType(mimeType)
                .extractedText(cleanedText)
                .uploadedAt(LocalDateTime.now())
                .user(user)
                .build();

        Document savedDocument = documentRepository.save(document);
        embeddingService.embedAndStore(savedDocument.getId(), savedDocument.getExtractedText());
        return savedDocument;
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument pdDocument = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdDocument);
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(file.getInputStream())) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : docx.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")  // collapse multiple spaces/newlines into one
                .replaceAll("[^\\x20-\\x7E\\n]", "")  // remove non-printable characters
                .trim();
    }

    public List<Document> getUserDocuments(User user) {
        return documentRepository.findByUser(user);
    }
}