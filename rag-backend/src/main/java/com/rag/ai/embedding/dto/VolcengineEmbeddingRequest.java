package com.rag.ai.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VolcengineEmbeddingRequest {
    
    @JsonProperty("model")
    public String model;
    
    @JsonProperty("input")
    public List<InputItem> input;
    
    @JsonProperty("encoding_format")
    public String encodingFormat;
    
    @JsonProperty("dimensions")
    public Integer dimensions;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<InputItem> getInput() { return input; }
    public void setInput(List<InputItem> input) { this.input = input; }
    public String getEncodingFormat() { return encodingFormat; }
    public void setEncodingFormat(String encodingFormat) { this.encodingFormat = encodingFormat; }
    public Integer getDimensions() { return dimensions; }
    public void setDimensions(Integer dimensions) { this.dimensions = dimensions; }

    public static class InputItem {
        @JsonProperty("type")
        public String type;
        
        @JsonProperty("text")
        public String text;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}