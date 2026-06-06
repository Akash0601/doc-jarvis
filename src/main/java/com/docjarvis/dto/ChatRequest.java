package com.docjarvis.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private Long documentId;
    private String question;
}