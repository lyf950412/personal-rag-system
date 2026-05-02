package com.rag.service;

import com.rag.constant.DocumentStatus;
import com.rag.dto.ConfirmUploadRequest;
import com.rag.dto.DocumentDTO;
import com.rag.dto.StsCredentialRequest;
import com.rag.entity.Document;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.DocumentRepository;
import com.rag.repository.KnowledgeBaseRepository;
import com.rag.service.storage.ObjectStorageService;
import com.rag.service.storage.StsCredential;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final TikaFileParserService tikaFileParserService;
    private final RAGService ragService;
    private final ObjectStorageService objectStorageService;

    private static final String STORAGE_PREFIX = "tos://";
    
    public List<DocumentDTO> getRecentDocuments(int limit) {
        return documentRepository.findRecentDocuments(PageRequest.of(0, limit)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<DocumentDTO> getDocumentsByKnowledgeBase(Long kbId) {
        return documentRepository.findByKnowledgeBaseId(kbId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public StsCredential getStsCredential(StsCredentialRequest request) {
        log.info("获取STS临时凭证 - fileName: {}, filePath: {}, fileSize: {}", 
                request.getFileName(), request.getFilePath(), request.getFileSize());
        return objectStorageService.getStsCredential(request);
    }

    @Transactional
    public DocumentDTO confirmUpload(ConfirmUploadRequest request) {
        log.info("确认上传完成 - objectKey: {}, filename: {}, size: {}", 
                request.getObjectKey(), request.getOriginalFilename(), request.getFileSize());

        KnowledgeBase kb = knowledgeBaseRepository.findById(request.getKnowledgeBaseId())
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + request.getKnowledgeBaseId()));

        Document doc = new Document();
        doc.setName(request.getOriginalFilename());
        doc.setType(request.getFileExtension());
        doc.setFileSize(formatFileSize(request.getFileSize()));
        doc.setFilePath(STORAGE_PREFIX + request.getObjectKey());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setKnowledgeBase(kb);

        Document saved = documentRepository.save(doc);

        log.info("Document confirmed - id: {}, objectKey: {}", saved.getId(), request.getObjectKey());

        tikaFileParserService.parseDocumentAsync(saved.getId(), request.getObjectKey(), request.getFileExtension());

        return toDTO(saved);
    }

    @Transactional
    public void updateDocumentStatus(Long id, String status) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + id));
        doc.setStatus(status);

        if (DocumentStatus.COMPLETED.equals(status)) {
            doc.setProcessedTime(java.time.LocalDateTime.now());
        }
        
        documentRepository.save(doc);
    }
    
    @Transactional
    public void deleteDocument(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + id));

        try {
            ragService.removeFromKnowledgeBase(String.valueOf(id));
            log.info("Removed document from vector store: {}", id);
        } catch (Exception e) {
            log.error("Failed to remove document from vector store: {}", id, e);
        }

        try {
            String filePath = doc.getFilePath();
            if (filePath != null && filePath.startsWith(STORAGE_PREFIX)) {
                String objectKey = filePath.substring(STORAGE_PREFIX.length());
                objectStorageService.delete(objectKey);
                log.info("Deleted file from object storage: {}", objectKey);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from object storage: {}", doc.getFilePath(), e);
        }

        documentRepository.delete(doc);
    }
    
    public Long countAllDocuments() {
        return documentRepository.countAllDocuments();
    }
    
    public Long sumAllVectorCount() {
        Long count = documentRepository.sumAllVectorCount();
        return count != null ? count : 0L;
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.2fKB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2fMB", size / (1024.0 * 1024));
        return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
    }
    
    private DocumentDTO toDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setName(doc.getName());
        dto.setType(doc.getType());
        dto.setFileSize(doc.getFileSize());
        dto.setStatus(doc.getStatus());
        dto.setChunkCount(doc.getChunkCount());
        dto.setVectorCount(doc.getVectorCount());
        dto.setUploadTime(doc.getUploadTime());
        dto.setProcessedTime(doc.getProcessedTime());
        
        if (doc.getKnowledgeBase() != null) {
            dto.setKnowledgeBaseId(doc.getKnowledgeBase().getId());
            dto.setKnowledgeBaseName(doc.getKnowledgeBase().getName());
        }
        
        return dto;
    }
}
