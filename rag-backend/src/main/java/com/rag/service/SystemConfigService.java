package com.rag.service;

import com.rag.entity.SystemConfig;
import com.rag.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigService {
    private final SystemConfigRepository configRepository;
    
    public SystemConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }
    
    public Map<String, Object> getAllConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("llmModel", getConfigValue("llm_model", "qwen2-7b"));
        configs.put("embeddingModel", getConfigValue("embedding_model", "bge-base-zh"));
        configs.put("temperature", Double.parseDouble(getConfigValue("temperature", "0.7")));
        configs.put("maxTokens", Integer.parseInt(getConfigValue("max_tokens", "2048")));
        configs.put("chunkSize", Integer.parseInt(getConfigValue("chunk_size", "500")));
        configs.put("chunkOverlap", Integer.parseInt(getConfigValue("chunk_overlap", "50")));
        configs.put("topK", Integer.parseInt(getConfigValue("top_k", "5")));
        configs.put("similarityThreshold", Double.parseDouble(getConfigValue("similarity_threshold", "0.7")));
        configs.put("systemName", getConfigValue("system_name", "全模态RAG知识库系统"));
        configs.put("maxFileSize", Integer.parseInt(getConfigValue("max_file_size", "500")));
        configs.put("enableAuditLog", Boolean.parseBoolean(getConfigValue("enable_audit_log", "true")));
        configs.put("enableNotifications", Boolean.parseBoolean(getConfigValue("enable_notifications", "true")));
        configs.put("autoIndex", Boolean.parseBoolean(getConfigValue("auto_index", "true")));
        configs.put("enableCache", Boolean.parseBoolean(getConfigValue("enable_cache", "true")));
        return configs;
    }
    
    public void updateConfig(String key, Object value) {
        String configKey = convertToConfigKey(key);
        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(configKey);
                    return newConfig;
                });
        config.setConfigValue(String.valueOf(value));
        configRepository.save(config);
    }
    
    public void updateConfigs(Map<String, Object> configs) {
        configs.forEach(this::updateConfig);
    }
    
    private String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }
    
    private String convertToConfigKey(String key) {
        return switch (key) {
            case "llmModel" -> "llm_model";
            case "embeddingModel" -> "embedding_model";
            case "temperature" -> "temperature";
            case "maxTokens" -> "max_tokens";
            case "chunkSize" -> "chunk_size";
            case "chunkOverlap" -> "chunk_overlap";
            case "topK" -> "top_k";
            case "similarityThreshold" -> "similarity_threshold";
            case "systemName" -> "system_name";
            case "maxFileSize" -> "max_file_size";
            case "enableAuditLog" -> "enable_audit_log";
            case "enableNotifications" -> "enable_notifications";
            case "autoIndex" -> "auto_index";
            case "enableCache" -> "enable_cache";
            default -> key;
        };
    }
}
