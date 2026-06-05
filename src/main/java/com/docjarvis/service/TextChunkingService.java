package com.docjarvis.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private static final int CHUNK_SIZE = 500;      // tokens per chunk
    private static final int CHUNK_OVERLAP = 50;    // overlapping tokens

    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Step 1: Clean and split text into individual words (our token approximation)
        String cleanedText = text.replaceAll("\\s+", " ").trim();
        String[] words = cleanedText.split(" ");

        List<String> chunks = new ArrayList<>();
        int start = 0;

        // Step 2: Slide a window of CHUNK_SIZE words across the word array
        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE, words.length);

            // Step 3: Join words back into a string for this chunk
            String chunk = String.join(" ", 
                java.util.Arrays.copyOfRange(words, start, end));

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            // Step 4: Move start forward by (CHUNK_SIZE - OVERLAP)
            // This creates the 50-word overlap with the next chunk
            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }

        return chunks;
    }

    public int getChunkCount(String text) {
        return chunkText(text).size();
    }
}