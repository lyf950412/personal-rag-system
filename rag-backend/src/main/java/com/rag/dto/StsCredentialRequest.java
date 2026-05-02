package com.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StsCredentialRequest {
    
    @NotNull
    @Schema(description = "文件名：如：200618193248.png")
    private String fileName;
    
    @NotNull
    @Schema(description = "文件路径：如：/data 或者 /data/file.jpg")
    private String filePath;
    
    @Schema(description = "待上传文件的hash值")
    private String hashCode;
    
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
}
