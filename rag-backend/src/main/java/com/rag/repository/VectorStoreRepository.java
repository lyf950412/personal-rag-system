package com.rag.repository;

import com.rag.entity.VectorStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VectorStoreRepository extends JpaRepository<VectorStore, Long>, VectorStoreRepositoryCustom {

    List<VectorStore> findByDocumentId(String documentId);

    @Modifying
    @Transactional
    void deleteByDocumentId(String documentId);
}
