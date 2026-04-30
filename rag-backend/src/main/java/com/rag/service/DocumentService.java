package com.rag.service;

import com.rag.constant.DocumentStatus;
import com.rag.dto.DocumentDTO;
import com.rag.entity.Document;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.DocumentRepository;
import com.rag.repository.KnowledgeBaseRepository;
import com.rag.util.FileValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final TikaFileParserService tikaFileParserService;
    private final RAGService ragService;

    private static final String UPLOAD_DIR = "uploads/";
    
    public List<DocumentDTO> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
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
    
    public DocumentDTO getDocumentById(Long id) {
        return documentRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("文档不存在: " + id));
    }
    
    @Transactional
    public DocumentDTO uploadDocument(MultipartFile file, Long knowledgeBaseId) throws IOException {
        log.info("Starting document upload - filename: {}, size: {}, KB: {}",
                file.getOriginalFilename(), file.getSize(), knowledgeBaseId);

        FileValidator.validateFile(file);

        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + knowledgeBaseId));

        String originalFilename = file.getOriginalFilename();
        String fileExtension = FileValidator.getFileExtension(originalFilename).toUpperCase();

        String storedFilename = UUID.randomUUID().toString() + "_" + sanitizeFilename(originalFilename);
        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();

        if (!uploadPath.startsWith(Paths.get(System.getProperty("user.dir")).toAbsolutePath())) {
            throw new SecurityException("Invalid upload path");
        }

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(storedFilename).normalize();

        if (!filePath.startsWith(uploadPath)) {
            throw new SecurityException("Invalid file path detected: path traversal attempt");
        }

        Files.copy(file.getInputStream(), filePath);
        
        Document doc = new Document();
        doc.setName(originalFilename);
        doc.setType(fileExtension);
        doc.setFileSize(formatFileSize(file.getSize()));
        doc.setFilePath(filePath.toString());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setKnowledgeBase(kb);

        Document saved = documentRepository.save(doc);

        log.info("Document uploaded successfully - id: {}, path: {}", saved.getId(), filePath);

        tikaFileParserService.parseDocumentAsync(saved.getId(), filePath, fileExtension);

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
            Path filePath = Paths.get(doc.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", doc.getFilePath(), e);
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
    
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
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
