package com.rag.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PgVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreService.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${ai.rag.retrieval.top-k:5}")
    private int topK;

    @Value("${ai.rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Transactional
    public void initializeVectorTable() {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS vector_store (
                    id BIGSERIAL PRIMARY KEY,
                    document_id VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    metadata TEXT,
                    embedding vector(1536),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
            
            String createIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_vector_store_embedding 
                ON vector_store USING ivfflat (embedding vector_ip_ops);
            """;
            
            String createDocIndexSql = """
                CREATE INDEX IF NOT EXISTS idx_vector_store_document_id 
                ON vector_store(document_id);
            """;

            entityManager.createNativeQuery(createTableSql).executeUpdate();
            entityManager.createNativeQuery(createIndexSql).executeUpdate();
            entityManager.createNativeQuery(createDocIndexSql).executeUpdate();
            
            log.info("PostgreSQL向量表初始化成功");
        } catch (Exception e) {
            log.error("初始化PostgreSQL向量表失败: {}", e.getMessage());
            throw new RuntimeException("初始化PostgreSQL向量表失败", e);
        }
    }

    @Transactional
    public void addDocument(String documentId, List<String> contentChunks, String metadata) {
        log.info("添加文档到PostgreSQL向量库 - documentId: {}, content length: {}", documentId, contentChunks.size());

        try {
//            String cleanedContent = cleanAndValidateText(content);
//            if (cleanedContent == null || cleanedContent.trim().isEmpty()) {
//                log.warn("文档内容为空，跳过向量化 - documentId: {}", documentId);
//                return;
//            }

            for (String contentChunk : contentChunks) {
                float[] embedding = generateEmbedding(contentChunk);
                String embeddingStr = arrayToString(embedding);

                String sql = """
                INSERT INTO vector_store (document_id, content, metadata, embedding)
                VALUES (:documentId, :content, :metadata, CAST(:embedding AS vector))
            """;
                entityManager.createNativeQuery(sql)
                        .setParameter("documentId", documentId)
                        .setParameter("content", contentChunk)
                        .setParameter("metadata", metadata != null ? metadata : "")
                        .setParameter("embedding", embeddingStr)
                        .executeUpdate();

                log.info("文档添加成功 - documentId: {}", documentId);
            }

        } catch (Exception e) {
            log.error("添加文档到PostgreSQL失败", e);
            throw new RuntimeException("添加文档到PostgreSQL失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> search(String query) {
        return search(query, topK);
    }

    @SuppressWarnings("unchecked")
    public List<String> search(String query, int topK) {
        log.info("PostgreSQL向量搜索 - query: {}, topK: {}", query, topK);

        try {
            float[] queryEmbedding = generateEmbedding(query);
            String embeddingStr = arrayToString(queryEmbedding);

            String sql = """
                SELECT content FROM vector_store
                ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                LIMIT :topK
            """;

            List<Object> results = entityManager.createNativeQuery(sql)
                    .setParameter("queryEmbedding", embeddingStr)
                    .setParameter("topK", topK)
                    .getResultList();

            List<String> contents = new ArrayList<>();
            for (Object result : results) {
                if (result != null) {
                    contents.add(result.toString());
                }
            }

            log.info("搜索完成 - 结果数: {}", contents.size());
            return contents;
        } catch (Exception e) {
            log.error("PostgreSQL向量搜索失败", e);
            throw new RuntimeException("PostgreSQL向量搜索失败", e);
        }
    }

    @Transactional
    public void deleteDocument(String documentId) {
        log.info("从PostgreSQL删除文档 - documentId: {}", documentId);

        try {
            String sql = "DELETE FROM vector_store WHERE document_id = :documentId";
            int deleted = entityManager.createNativeQuery(sql)
                    .setParameter("documentId", documentId)
                    .executeUpdate();

            log.info("文档删除成功 - documentId: {}, 删除行数: {}", documentId, deleted);
        } catch (Exception e) {
            log.error("从PostgreSQL删除文档失败", e);
            throw new RuntimeException("从PostgreSQL删除文档失败", e);
        }
    }

    @Transactional
    public void clear() {
        log.info("清空PostgreSQL向量库");

        try {
            entityManager.createNativeQuery("TRUNCATE TABLE vector_store").executeUpdate();
            log.info("PostgreSQL向量库已清空");
        } catch (Exception e) {
            log.error("清空PostgreSQL向量库失败", e);
            throw new RuntimeException("清空PostgreSQL向量库失败", e);
        }
    }

    private float[] generateEmbedding(String text) {
        try {
            log.debug("生成embedding - 文本长度: {}", text.length());
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("生成embedding失败 - 文本长度: {}, 错误: {}", text.length(), e.getMessage());
            throw new RuntimeException("生成embedding失败: " + e.getMessage(), e);
        }
    }
    private List<float[]> generateEmbeddingList(List<String> texts) {
        try {
            log.debug("生成embedding - 文本长度: {}", texts.size());
            return embeddingModel.embed(texts);
        } catch (Exception e) {
            log.error("生成embedding失败 - 文本长度: {}, 错误: {}", texts.size(), e.getMessage());
            throw new RuntimeException("生成embedding失败: " + e.getMessage(), e);
        }
    }

    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String cleanAndValidateText(String text) {
        if (text == null) {
            return null;
        }
        
        String cleaned = text.trim();
        
        if (cleaned.isEmpty()) {
            return null;
        }
        
        cleaned = cleaned.replaceAll("\\s+", " ");
        
        if (cleaned.length() > 8000) {
            log.warn("文本过长({}字符)，截断至8000字符", cleaned.length());
            cleaned = cleaned.substring(0, 8000);
        }
        
        return cleaned;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
