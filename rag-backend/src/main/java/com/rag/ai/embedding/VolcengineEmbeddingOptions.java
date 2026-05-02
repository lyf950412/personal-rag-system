package com.rag.ai.embedding;

public class VolcengineEmbeddingOptions {
    
    private String model;
    private Integer dimensions;
    private String encodingFormat;
    private String baseUrl;
    private String apiKey;

    public VolcengineEmbeddingOptions() {
    }

    public VolcengineEmbeddingOptions(String model, Integer dimensions, String encodingFormat, String baseUrl, String apiKey) {
        this.model = model;
        this.dimensions = dimensions;
        this.encodingFormat = encodingFormat;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    public String getEncodingFormat() {
        return encodingFormat;
    }

    public void setEncodingFormat(String encodingFormat) {
        this.encodingFormat = encodingFormat;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private Integer dimensions;
        private String encodingFormat;
        private String baseUrl;
        private String apiKey;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder dimensions(Integer dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder encodingFormat(String encodingFormat) {
            this.encodingFormat = encodingFormat;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public VolcengineEmbeddingOptions build() {
            return new VolcengineEmbeddingOptions(model, dimensions, encodingFormat, baseUrl, apiKey);
        }
    }
}
