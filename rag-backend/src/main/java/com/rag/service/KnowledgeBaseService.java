package com.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rag.dto.KnowledgeBaseDTO;
import com.rag.entity.KnowledgeBase;
import com.rag.repository.KnowledgeBaseRepository;
import com.rag.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    
    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }
    
    public List<KnowledgeBaseDTO> getAllKnowledgeBases() {
        return knowledgeBaseRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    public KnowledgeBaseDTO getKnowledgeBaseById(Long id) {
        return knowledgeBaseRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("知识库不存在: " + id));
    }
    
    @Transactional
    public KnowledgeBaseDTO createKnowledgeBase(KnowledgeBaseDTO dto) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        kb.setOwner(dto.getOwner());
        kb.setTags(JsonUtil.toJson(dto.getTags()));
        kb.setStatus("正常");
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        log.info("Created knowledge base: {}", saved.getId());
        return toDTO(saved);
    }
    
    @Transactional
    public KnowledgeBaseDTO updateKnowledgeBase(Long id, KnowledgeBaseDTO dto) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在: " + id));
        if (dto.getName() != null) kb.setName(dto.getName());
        if (dto.getDescription() != null) kb.setDescription(dto.getDescription());
        if (dto.getOwner() != null) kb.setOwner(dto.getOwner());
        if (dto.getTags() != null) kb.setTags(JsonUtil.toJson(dto.getTags()));
        KnowledgeBase updated = knowledgeBaseRepository.save(kb);
        log.info("Updated knowledge base: {}", id);
        return toDTO(updated);
    }
    
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new RuntimeException("知识库不存在: " + id);
        }
        knowledgeBaseRepository.deleteById(id);
        log.info("Deleted knowledge base: {}", id);
    }
    
    public List<KnowledgeBaseDTO> searchKnowledgeBases(String keyword) {
        return knowledgeBaseRepository.findByNameContaining(keyword).stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    private KnowledgeBaseDTO toDTO(KnowledgeBase kb) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setOwner(kb.getOwner());
        dto.setStatus(kb.getStatus());
        dto.setTags(JsonUtil.fromJson(kb.getTags(), new TypeReference<List<String>>() {
        }));
        Long docCount = knowledgeBaseRepository.countDocumentsByKnowledgeBaseId(kb.getId());
        Long vectorCount = knowledgeBaseRepository.sumVectorCountByKnowledgeBaseId(kb.getId());
        dto.setDocCount(docCount != null ? docCount : 0L);
        dto.setVectorCount(vectorCount != null ? vectorCount : 0L);
        return dto;
    }
}
