package com.docjarvis.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByDocumentIdOrderByTimestampAsc(Long documentId);

    @Transactional
    void deleteByDocumentId(Long documentId);
}