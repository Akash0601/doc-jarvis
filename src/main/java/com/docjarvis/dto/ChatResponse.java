package com.docjarvis.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String question;
    private String answer;
    private String sourceDocument;
    private Integer pageNumber;
    private LocalDateTime timestamp;
}