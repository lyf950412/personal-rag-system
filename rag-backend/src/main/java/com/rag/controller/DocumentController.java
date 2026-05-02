package com.rag.controller;

import com.rag.config.TosConfig;
import com.rag.dto.ApiResponse;
import com.rag.dto.ConfirmUploadRequest;
import com.rag.dto.DocumentDTO;
import com.rag.dto.StsCredentialRequest;
import com.rag.service.DocumentService;
import com.rag.service.storage.StsCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档管理", description = "文档上传、查询、删除等接口")
public class DocumentController {
    
    private final DocumentService documentService;
    private final TosConfig tosConfig;
    
    public DocumentController(DocumentService documentService, TosConfig tosConfig) {
        this.documentService = documentService;
        this.tosConfig = tosConfig;
    }
    
    @GetMapping("/recent")
    @Operation(summary = "获取最近文档", description = "获取最近上传的文档列表")
    @Parameter(name = "limit", description = "返回数量限制", required = false)
    public ApiResponse<List<DocumentDTO>> getRecentDocuments(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(documentService.getRecentDocuments(limit));
    }
    
    @GetMapping("/knowledge-base/{kbId}")
    @Operation(summary = "按知识库获取文档", description = "获取指定知识库中的所有文档")
    @Parameter(name = "kbId", description = "知识库ID", required = true)
    public ApiResponse<List<DocumentDTO>> getDocumentsByKnowledgeBase(@PathVariable Long kbId) {
        return ApiResponse.success(documentService.getDocumentsByKnowledgeBase(kbId));
    }
    
    @PostMapping("/sts-credential")
    @Operation(summary = "获取STS临时凭证", description = "获取用于前端直传对象存储的临时凭证")
    public ApiResponse<Map<String, Object>> getStsCredential(@Valid @RequestBody StsCredentialRequest request) {
        StsCredential credential = documentService.getStsCredential(request);
        String objectKey = tosConfig.generateObjectKey(request.getFileName(), request.getFilePath());
        return ApiResponse.success("获取STS凭证成功", Map.of(
                "accessKeyId", credential.getAccessKeyId(),
                "secretAccessKey", credential.getSecretAccessKey(),
                "sessionToken", credential.getSessionToken(),
                "expiration", credential.getExpiration(),
                "bucketName", credential.getBucketName(),
                "objectKey", objectKey
        ));
    }
    
    @PostMapping("/confirm-upload")
    @Operation(summary = "确认上传完成", description = "客户端使用STS凭证上传完成后调用此接口确认")
    public ApiResponse<DocumentDTO> confirmUpload(@Valid @RequestBody ConfirmUploadRequest request) {
        return ApiResponse.success("文件上传确认成功", 
                documentService.confirmUpload(request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "根据文档ID删除文档及其相关向量数据")
    @Parameter(name = "id", description = "文档ID", required = true)
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("文档删除成功", null);
    }
}
