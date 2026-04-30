package com.rag.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DashboardStatsDTO {
    private Long knowledgeBaseCount = 0L;
    private Long documentCount = 0L;
    private Long vectorCount = 0L;
    private Long chatCount = 0L;
    
    public Long getKnowledgeBaseCount() {
        return knowledgeBaseCount;
    }
    
    public void setKnowledgeBaseCount(Long knowledgeBaseCount) {
        this.knowledgeBaseCount = knowledgeBaseCount != null ? knowledgeBaseCount : 0L;
    }
    
    public Long getDocumentCount() {
        return documentCount;
    }
    
    public void setDocumentCount(Long documentCount) {
        this.documentCount = documentCount != null ? documentCount : 0L;
    }
    
    public Long getVectorCount() {
        return vectorCount;
    }
    
    public void setVectorCount(Long vectorCount) {
        this.vectorCount = vectorCount != null ? vectorCount : 0L;
    }
    
    public Long getChatCount() {
        return chatCount;
    }
    
    public void setChatCount(Long chatCount) {
        this.chatCount = chatCount != null ? chatCount : 0L;
    }
}
