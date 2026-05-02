package com.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document", indexes = {
    @Index(name = "idx_upload_time", columnList = "uploadTime DESC"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_knowledge_base", columnList = "knowledge_base_id"),
    @Index(name = "idx_upload_time_status", columnList = "uploadTime DESC, status")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(length = 50)
    private String type;

    @Column(length = 100)
    private String fileSize;

    @Column(length = 1000)
    private String filePath;

    @Column(length = 20)
    private String status = com.rag.constant.DocumentStatus.PENDING;

    private Integer chunkCount = 0;

    private Integer vectorCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime uploadTime;

    private LocalDateTime processedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getVectorCount() { return vectorCount; }
    public void setVectorCount(Integer vectorCount) { this.vectorCount = vectorCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public LocalDateTime getProcessedTime() { return processedTime; }
    public void setProcessedTime(LocalDateTime processedTime) { this.processedTime = processedTime; }
    public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) { this.knowledgeBase = knowledgeBase; }
}
