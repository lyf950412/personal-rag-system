package com.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final EmbeddingModel embeddingModel;
    private final Map<String, StoredDocument> documents = new ConcurrentHashMap<>();
    private final List<String> documentOrder = Collections.synchronizedList(new ArrayList<>());

    @Value("${ai.rag.retrieval.top-k:5}")
    private int topK;

    @Value("${ai.rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${ai.vector-store.max-segments:10000}")
    private int maxSegments;

    public VectorStoreService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void addDocument(String documentId, String content, String metadata) {
        log.info("Adding document to vector store - id: {}, content length: {}", documentId, content.length());

        try {
            if (documents.size() >= maxSegments) {
                log.warn("Vector store capacity reached, clearing oldest data");
                clearOldestDocument();
            }

            String[] chunks = splitIntoChunks(content);
            
            for (int i = 0; i < chunks.length; i++) {
                String chunkId = documentId + "_chunk_" + i;
                float[] embedding = generateEmbedding(chunks[i]);
                
                StoredDocument chunk = new StoredDocument(
                        chunkId,
                        chunks[i],
                        embedding,
                        documentId,
                        metadata
                );
                documents.put(chunkId, chunk);
            }

            documentOrder.add(documentId);
            log.info("Document added successfully - chunks: {}, total chunks: {}", chunks.length, documents.size());
        } catch (Exception e) {
            log.error("Error adding document to vector store", e);
            throw new RuntimeException("Failed to add document to vector store", e);
        }
    }

    public List<String> search(String query) {
        return search(query, topK);
    }

    public List<String> search(String query, int topK) {
        log.info("Searching vector store - query: {}, topK: {}", query, topK);

        try {
            float[] queryEmbedding = generateEmbedding(query);
            
            List<SearchResult> results = documents.values().stream()
                    .map(doc -> new SearchResult(doc.text, cosineSimilarity(queryEmbedding, doc.embedding)))
                    .filter(result -> result.similarity >= similarityThreshold)
                    .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                    .limit(topK)
                    .collect(Collectors.toList());

            List<String> texts = results.stream()
                    .map(r -> r.text)
                    .collect(Collectors.toList());

            log.info("Search completed - results: {}", texts.size());
            return texts;
        } catch (Exception e) {
            log.error("Error searching vector store", e);
            throw new RuntimeException("Failed to search vector store", e);
        }
    }

    public void deleteDocument(String documentId) {
        log.info("Deleting document from vector store - id: {}", documentId);

        try {
            List<String> keysToRemove = documents.values().stream()
                    .filter(doc -> documentId.equals(doc.parentDocumentId))
                    .map(doc -> doc.id)
                    .collect(Collectors.toList());

            keysToRemove.forEach(documents::remove);
            documentOrder.remove(documentId);

            log.info("Document deleted successfully - chunks removed: {}, remaining: {}", keysToRemove.size(), documents.size());
        } catch (Exception e) {
            log.error("Error deleting document from vector store", e);
            throw new RuntimeException("Failed to delete document from vector store", e);
        }
    }

    public void clear() {
        log.info("Clearing vector store");
        documents.clear();
        documentOrder.clear();
        log.info("Vector store cleared successfully");
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getCurrentChunkCount() {
        return documents.size();
    }

    public int getMaxChunks() {
        return maxSegments;
    }

    private float[] generateEmbedding(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    private String[] splitIntoChunks(String content) {
        int chunkSize = 500;
        int overlap = 50;
        
        if (content.length() <= chunkSize) {
            return new String[]{content};
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            
            if (start + chunkSize < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int breakpoint = Math.max(lastPeriod, lastNewline);
                
                if (breakpoint > start + chunkSize / 2) {
                    end = breakpoint + 1;
                }
            }
            
            chunks.add(content.substring(start, end));
            start = end - overlap;
        }

        return chunks.toArray(new String[0]);
    }

    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    private void clearOldestDocument() {
        if (!documentOrder.isEmpty()) {
            String oldestDocId = documentOrder.remove(0);
            deleteDocument(oldestDocId);
        }
    }

    private static class StoredDocument {
        String id;
        String text;
        float[] embedding;
        String parentDocumentId;
        String metadata;

        StoredDocument(String id, String text, float[] embedding, String parentDocumentId, String metadata) {
            this.id = id;
            this.text = text;
            this.embedding = embedding;
            this.parentDocumentId = parentDocumentId;
            this.metadata = metadata;
        }
    }

    private static class SearchResult {
        String text;
        double similarity;

        SearchResult(String text, double similarity) {
            this.text = text;
            this.similarity = similarity;
        }
    }
}
