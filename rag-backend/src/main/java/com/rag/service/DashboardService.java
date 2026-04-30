package com.rag.service;

import com.rag.dto.DashboardStatsDTO;
import com.rag.repository.DocumentRepository;
import com.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    
    public DashboardService(KnowledgeBaseRepository knowledgeBaseRepository, DocumentRepository documentRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
    }
    
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setKnowledgeBaseCount(knowledgeBaseRepository.count());
        stats.setDocumentCount(documentRepository.countAllDocuments());
        Long vectorCount = documentRepository.sumAllVectorCount();
        stats.setVectorCount(vectorCount != null ? vectorCount : 0L);
        stats.setChatCount(8542L);
        return stats;
    }
}
