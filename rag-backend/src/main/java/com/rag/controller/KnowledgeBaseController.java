package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.dto.KnowledgeBaseDTO;
import com.rag.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {
    
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);
    private final KnowledgeBaseService knowledgeBaseService;
    
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }
    
    @GetMapping
    public ApiResponse<List<KnowledgeBaseDTO>> getAllKnowledgeBases() {
        log.debug("Getting all knowledge bases");
        return ApiResponse.success(knowledgeBaseService.getAllKnowledgeBases());
    }
    
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> getKnowledgeBaseById(@PathVariable Long id) {
        log.debug("Getting knowledge base by id: {}", id);
        return ApiResponse.success(knowledgeBaseService.getKnowledgeBaseById(id));
    }
    
    @PostMapping
    public ApiResponse<KnowledgeBaseDTO> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseDTO dto) {
        log.info("Creating new knowledge base: {}", dto.getName());
        return ApiResponse.success("知识库创建成功", knowledgeBaseService.createKnowledgeBase(dto));
    }
    
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> updateKnowledgeBase(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseDTO dto) {
        log.info("Updating knowledge base: {}", id);
        return ApiResponse.success("知识库更新成功", knowledgeBaseService.updateKnowledgeBase(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long id) {
        log.info("Deleting knowledge base: {}", id);
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.success("知识库删除成功", null);
    }
    
    @GetMapping("/search")
    public ApiResponse<List<KnowledgeBaseDTO>> searchKnowledgeBases(@RequestParam String keyword) {
        log.debug("Searching knowledge bases with keyword: {}", keyword);
        return ApiResponse.success(knowledgeBaseService.searchKnowledgeBases(keyword));
    }
}
