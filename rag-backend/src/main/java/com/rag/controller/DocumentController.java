package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.dto.DocumentDTO;
import com.rag.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档管理", description = "文档上传、查询、删除等接口")
public class DocumentController {
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @GetMapping
    @Operation(summary = "获取所有文档", description = "获取系统中所有上传的文档列表")
    public ApiResponse<List<DocumentDTO>> getAllDocuments() {
        return ApiResponse.success(documentService.getAllDocuments());
    }
    
    @GetMapping("/recent")
    @Operation(summary = "获取最近文档", description = "获取最近上传的文档列表")
    @Parameter(name = "limit", description = "返回数量限制", required = false)
    public ApiResponse<List<DocumentDTO>> getRecentDocuments(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(documentService.getRecentDocuments(limit));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据文档ID获取文档详细信息")
    @Parameter(name = "id", description = "文档ID", required = true)
    public ApiResponse<DocumentDTO> getDocumentById(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocumentById(id));
    }
    
    @GetMapping("/knowledge-base/{kbId}")
    @Operation(summary = "按知识库获取文档", description = "获取指定知识库中的所有文档")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    public ApiResponse<List<DocumentDTO>> getDocumentsByKnowledgeBase(@PathVariable Long kbId) {
        return ApiResponse.success(documentService.getDocumentsByKnowledgeBase(kbId));
    }
    
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传文档到指定知识库，支持PDF、Word、TXT等格式")
    @Parameter(name = "file", description = "要上传的文件", required = true)
    @Parameter(name = "knowledgeBaseId", description = "目标知识库ID", required = true)
    public ApiResponse<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId) throws IOException {
        return ApiResponse.success("文件上传成功", documentService.uploadDocument(file, knowledgeBaseId));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "根据文档ID删除文档及其相关向量数据")
    @Parameter(name = "id", description = "文档ID", required = true)
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("文档删除成功", null);
    }
}
