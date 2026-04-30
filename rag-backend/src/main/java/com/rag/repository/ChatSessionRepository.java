package com.rag.repository;

import com.rag.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    Optional<ChatSession> findBySessionId(String sessionId);
    
    List<ChatSession> findByUserIdOrderByLastActivityDesc(Long userId);
    
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId ORDER BY s.lastActivity DESC")
    List<ChatSession> findRecentSessionsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT s FROM ChatSession s WHERE s.lastActivity < :expiryTime")
    List<ChatSession> findExpiredSessions(@Param("expiryTime") LocalDateTime expiryTime);
    
    void deleteBySessionId(String sessionId);
    
    long countByUserId(Long userId);
}
