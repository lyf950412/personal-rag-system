package com.rag.service;

import com.rag.constant.DocumentStatus;
import com.rag.entity.Document;
import com.rag.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class TikaFileParserService {

    private static final Logger log = LoggerFactory.getLogger(TikaFileParserService.class);
    
    private final DocumentRepository documentRepository;
    private final PgVectorStoreService vectorStoreService;
    private final RAGService ragService;
    private final DocumentReaderService documentReaderService;

    public TikaFileParserService(DocumentRepository documentRepository, 
                                  PgVectorStoreService vectorStoreService, 
                                  RAGService ragService,
                                  DocumentReaderService documentReaderService) {
        this.documentRepository = documentRepository;
        this.vectorStoreService = vectorStoreService;
        this.ragService = ragService;
        this.documentReaderService = documentReaderService;
    }

    @Async("documentProcessorExecutor")
    public void parseDocumentAsync(Long documentId, Path filePath, String fileExtension) {
        try {
            log.info("Starting document parsing with DocumentReaderService: {}, type: {}", documentId, fileExtension);
            updateDocumentStatus(documentId, DocumentStatus.PROCESSING);
            
            List<String> contentChunks = documentReaderService.parseAndSplitDocument(filePath.toFile());
            log.info("文档分割完成 - documentId: {}, 块数: {}", documentId, contentChunks.size());
            
            String content = String.join("\n\n", contentChunks);
            
            Document doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) {
                doc.setContent(content);
                int chunkCount = contentChunks.size();
                doc.setChunkCount(chunkCount);
                doc.setVectorCount(chunkCount);
                documentRepository.save(doc);

                if (content != null && !content.isEmpty()) {
                    log.info("开始向量化 - documentId: {}, 内容长度: {}", documentId, content.length());
                    ragService.addToKnowledgeBase(
                            doc.getKnowledgeBase().getId(),
                            String.valueOf(documentId),
                            contentChunks
                    );
                    log.info("文档向量化成功 - documentId: {}", documentId);
                } else {
                    log.warn("文档内容为空，跳过向量化 - documentId: {}", documentId);
                }
            }
            
            updateDocumentStatus(documentId, DocumentStatus.COMPLETED);
            log.info("Document parsing completed: {}, chunks: {}", documentId, contentChunks.size());
            
        } catch (Exception e) {
            log.error("Document parsing failed: " + documentId, e);
            updateDocumentStatusWithError(documentId, e.getMessage());
        }
    }

    public String detectContentType(Path filePath) {
        try {
            String filename = filePath.getFileName().toString().toLowerCase();
            if (filename.endsWith(".pdf")) {
                return "application/pdf";
            } else if (filename.endsWith(".doc") || filename.endsWith(".docx")) {
                return "application/msword";
            } else if (filename.endsWith(".txt")) {
                return "text/plain";
            } else if (filename.endsWith(".ppt") || filename.endsWith(".pptx")) {
                return "application/vnd.ms-powerpoint";
            } else if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
                return "application/vnd.ms-excel";
            }
            return "application/octet-stream";
        } catch (Exception e) {
            log.error("Content type detection failed", e);
            return "application/octet-stream";
        }
    }

    private int calculateChunkCount(String content) {
        if (content == null || content.isEmpty()) return 0;
        int chunkSize = 500;
        int overlap = 50;
        int effectiveChunkSize = chunkSize - overlap;
        return Math.max(1, (int) Math.ceil((double) content.length() / effectiveChunkSize));
    }

    private void updateDocumentStatus(Long id, String status) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(status);
            if (DocumentStatus.COMPLETED.equals(status)) {
                doc.setProcessedTime(java.time.LocalDateTime.now());
            }
            documentRepository.save(doc);
        });
    }

    private void updateDocumentStatusWithError(Long id, String error) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(error);
            documentRepository.save(doc);
        });
    }
}
