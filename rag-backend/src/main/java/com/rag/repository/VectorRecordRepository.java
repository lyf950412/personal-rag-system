package com.rag.repository;

import com.rag.entity.VectorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VectorRecordRepository extends JpaRepository<VectorRecord, Long> {
    
    List<VectorRecord> findByDocumentId(Long documentId);
    
    @Query("SELECT COUNT(v) FROM VectorRecord v")
    Long countAllVectors();
}
