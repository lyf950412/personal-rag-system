package com.rag.repository;

import com.rag.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    List<KnowledgeBase> findByNameContaining(String name);
    
    List<KnowledgeBase> findByOwner(String owner);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.knowledgeBase.id = ?1")
    Long countDocumentsByKnowledgeBaseId(Long kbId);
    
    @Query("SELECT SUM(d.vectorCount) FROM Document d WHERE d.knowledgeBase.id = ?1")
    Long sumVectorCountByKnowledgeBaseId(Long kbId);
}
