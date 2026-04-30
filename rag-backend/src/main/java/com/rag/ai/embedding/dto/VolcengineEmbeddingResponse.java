package com.rag.ai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolcengineEmbeddingResponse {
    
    private long created;
    private String id;
    private String model;
    private String object;
    private Data data;
    private Usage usage;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Data
    public static class Data {
        private String object;
        private List<Float> embedding;
        private List<SparseEmbeddingItem> sparse_embedding;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SparseEmbeddingItem {
        private int index;
        private float value;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int promptTokens;
        @JsonProperty("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;
        private int totalTokens;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptTokensDetails {
        private int imageTokens;
        private int textTokens;
    }
}
