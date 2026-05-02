package com.rag.service;

import com.rag.entity.VectorStore;
import com.rag.repository.VectorStoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.util.JsonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PgVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreService.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStoreRepository vectorStoreRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.rag.retrieval.top-k:5}")
    private int topK;

    @Value("${ai.rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @EventListener(ApplicationReadyEvent.class)
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
    public void addDocument(String documentId, List<Document> contentChunks) {
        log.info("添加文档到PostgreSQL向量库 - documentId: {}, chunks: {}", documentId, contentChunks.size());

        try {
            int batchSize = 50;
            List<VectorStore> batchToSave = new ArrayList<>();

            for (int i = 0; i < contentChunks.size(); i += batchSize) {
                int end = Math.min(i + batchSize, contentChunks.size());
                List<Document> chunkBatch = contentChunks.subList(i, end);

                List<String> texts = chunkBatch.stream().map(Document::getText).collect(Collectors.toList());
                List<float[]> embeddings = generateEmbeddings(texts);

                for (int j = 0; j < chunkBatch.size(); j++) {
                    Document doc = chunkBatch.get(j);
                    VectorStore vectorStore = new VectorStore();
                    vectorStore.setDocumentId(documentId);
                    vectorStore.setContent(doc.getText());
                    vectorStore.setMetadata(JsonUtil.toJson(doc.getMetadata()));
                    vectorStore.setEmbedding(arrayToString(embeddings.get(j)));
                    batchToSave.add(vectorStore);
                }

                vectorStoreRepository.saveAll(batchToSave);
                entityManager.flush();
                entityManager.clear();
                batchToSave.clear();
                log.debug("已批量保存 {} 个向量", end);
            }

            log.info("文档添加成功 - documentId: {}, total chunks: {}", documentId, contentChunks.size());
        } catch (Exception e) {
            log.error("添加文档到PostgreSQL失败", e);
            throw new RuntimeException("添加文档到PostgreSQL失败", e);
        }
    }

    @Transactional
    public void addDocuments(List<DocumentBatch> batch) {
        log.info("批量添加文档到PostgreSQL向量库 - 批次大小: {}", batch.size());

        try {
            int totalChunks = 0;
            int batchSize = 50;
            List<VectorStore> batchToSave = new ArrayList<>();

            for (DocumentBatch doc : batch) {
                for (String contentChunk : doc.contentChunks()) {
                    float[] embedding = generateEmbedding(contentChunk);
                    String embeddingStr = arrayToString(embedding);

                    VectorStore vectorStore = new VectorStore();
                    vectorStore.setDocumentId(doc.documentId());
                    vectorStore.setContent(contentChunk);
                    vectorStore.setMetadata(doc.metadata() != null ? doc.metadata() : "");
                    vectorStore.setEmbedding(embeddingStr);

                    batchToSave.add(vectorStore);
                    totalChunks++;

                    if (batchToSave.size() >= batchSize) {
                        vectorStoreRepository.saveAll(batchToSave);
                        entityManager.flush();
                        entityManager.clear();
                        batchToSave.clear();
                        log.debug("已批量保存 {} 个向量", totalChunks);
                    }
                }
            }

            if (!batchToSave.isEmpty()) {
                vectorStoreRepository.saveAll(batchToSave);
            }

            log.info("批量文档添加成功 - 总chunks: {}", totalChunks);
        } catch (Exception e) {
            log.error("批量添加文档到PostgreSQL失败", e);
            throw new RuntimeException("批量添加文档到PostgreSQL失败", e);
        }
    }

    public List<String> search(String query) {
        return search(query, topK);
    }

    public List<String> search(String query, int topK) {
        log.info("PostgreSQL向量搜索 - query: {}, topK: {}", query, topK);

        try {
            float[] queryEmbedding = generateEmbedding(query);
            String embeddingStr = arrayToString(queryEmbedding);

            List<String> results = vectorStoreRepository.searchBySimilarity(embeddingStr, topK);

            log.info("搜索完成 - 结果数: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("PostgreSQL向量搜索失败", e);
            throw new RuntimeException("PostgreSQL向量搜索失败", e);
        }
    }

    @Transactional
    public void deleteDocument(String documentId) {
        log.info("从PostgreSQL删除文档 - documentId: {}", documentId);

        try {
            vectorStoreRepository.deleteByDocumentId(documentId);
            log.info("文档删除成功 - documentId: {}", documentId);
        } catch (Exception e) {
            log.error("从PostgreSQL删除文档失败", e);
            throw new RuntimeException("从PostgreSQL删除文档失败", e);
        }
    }

    @Transactional
    public void clear() {
        log.info("清空PostgreSQL向量库");

        try {
            vectorStoreRepository.deleteAll();
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

    private List<float[]> generateEmbeddings(List<String> texts) {
        try {
            return texts.stream()
                    .map(this::generateEmbedding)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("批量生成embedding失败", e);
            throw new RuntimeException("批量生成embedding失败: " + e.getMessage(), e);
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


    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public record DocumentBatch(String documentId, List<String> contentChunks, String metadata) {}
}
