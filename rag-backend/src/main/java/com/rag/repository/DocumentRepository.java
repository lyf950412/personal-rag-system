package com.rag.repository;

import com.rag.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByKnowledgeBaseId(Long knowledgeBaseId);
    
    Page<Document> findByKnowledgeBaseId(Long knowledgeBaseId, Pageable pageable);
    
    List<Document> findByStatus(String status);
    
    @Query("SELECT d FROM Document d ORDER BY d.uploadTime DESC")
    List<Document> findRecentDocuments(Pageable pageable);
    
    @Query("SELECT COUNT(d) FROM Document d")
    Long countAllDocuments();
    
    @Query("SELECT SUM(d.vectorCount) FROM Document d")
    Long sumAllVectorCount();
}
