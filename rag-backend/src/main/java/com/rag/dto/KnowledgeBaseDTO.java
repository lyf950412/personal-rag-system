package com.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeBaseDTO {
    private Long id;
    
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 200, message = "知识库名称长度不能超过200字符")
    private String name;
    
    @Size(max = 1000, message = "描述长度不能超过1000字符")
    private String description;
    
    @Size(max = 100, message = "负责人长度不能超过100字符")
    private String owner;
    
    private List<String> tags;

    private String status;

    private Long docCount;

    private Long vectorCount;
    
}
