package com.rag.dto;

import java.time.LocalDateTime;

public class DocumentDTO {
    private Long id;
    private String name;
    private String type;
    private String fileSize;
    private String status;
    private Integer chunkCount;
    private Integer vectorCount;
    private LocalDateTime uploadTime;
    private LocalDateTime processedTime;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getVectorCount() { return vectorCount; }
    public void setVectorCount(Integer vectorCount) { this.vectorCount = vectorCount; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public LocalDateTime getProcessedTime() { return processedTime; }
    public void setProcessedTime(LocalDateTime processedTime) { this.processedTime = processedTime; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getKnowledgeBaseName() { return knowledgeBaseName; }
    public void setKnowledgeBaseName(String knowledgeBaseName) { this.knowledgeBaseName = knowledgeBaseName; }
}
