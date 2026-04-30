package com.rag.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDTO {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime timestamp;
    private List<SourceDTO> sources;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public List<SourceDTO> getSources() { return sources; }
    public void setSources(List<SourceDTO> sources) { this.sources = sources; }
    
    public static class SourceDTO {
        private String title;
        private String page;
        private Double score;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPage() { return page; }
        public void setPage(String page) { this.page = page; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
