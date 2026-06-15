package com.docjarvis.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docjarvis.dto.FlashcardCard;
import com.docjarvis.dto.FlashcardRequest;
import com.docjarvis.service.FlashcardService;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<FlashcardCard>> generateFlashcards(
            @RequestBody FlashcardRequest request) {
        List<FlashcardCard> cards = flashcardService.generateFlashcards(request.getDocumentId());
        return ResponseEntity.ok(cards);
    }
}