package com.rag.repository;

import com.rag.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    @Query(value = "SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findRecentMessagesBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);
    
    void deleteBySessionId(Long sessionId);
    
    long countBySessionId(Long sessionId);
}
