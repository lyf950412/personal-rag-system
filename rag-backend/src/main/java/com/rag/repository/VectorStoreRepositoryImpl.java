package com.rag.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VectorStoreRepositoryImpl implements VectorStoreRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchBySimilarity(String queryEmbedding, int topK) {
        String sql = """
            SELECT content FROM vector_store
            ORDER BY embedding <=> CAST(?1 AS vector)
            LIMIT ?2
            """;
        return entityManager.createNativeQuery(sql)
                .setParameter(1, queryEmbedding)
                .setParameter(2, topK)
                .getResultList();
    }
}
