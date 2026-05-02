package com.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmUploadRequest {
    
    @NotNull
    @Schema(description = "对象存储Key")
    private String objectKey;
    
    @NotNull
    @Schema(description = "原始文件名")
    private String originalFilename;
    
    @NotNull
    @Schema(description = "文件扩展名")
    private String fileExtension;
    
    @NotNull
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;
    
    @NotNull
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
}
