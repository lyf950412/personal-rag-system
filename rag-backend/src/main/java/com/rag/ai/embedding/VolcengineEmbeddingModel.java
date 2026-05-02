package com.rag.ai.embedding;

import com.rag.ai.embedding.dto.VolcengineEmbeddingRequest;
import com.rag.ai.embedding.dto.VolcengineEmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VolcengineEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(VolcengineEmbeddingModel.class);

    private final RestTemplate restTemplate;
    private final VolcengineEmbeddingOptions options;

    public VolcengineEmbeddingModel(VolcengineEmbeddingOptions options) {
        this.restTemplate = new RestTemplate();
        this.options = options;
    }

    @Override
    public float[] embed(String text) {
        return embedText(text);
    }

    @Override
    public float[] embed(Document document) {
        return embedText(document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        List<Embedding> embeddings = IntStream.range(0, texts.size())
                .mapToObj(i -> {
                    float[] embedding = embedText(texts.get(i));
                    return new Embedding(embedding, i);
                })
                .collect(Collectors.toList());
        
        return new EmbeddingResponse(embeddings);
    }

    private float[] embedText(String text) {
        String url = options.getBaseUrl() + "/embeddings/multimodal";
        
        VolcengineEmbeddingRequest.InputItem inputItem = new VolcengineEmbeddingRequest.InputItem();
        inputItem.setType("text");
        inputItem.setText(text);
        
        VolcengineEmbeddingRequest embeddingRequest = new VolcengineEmbeddingRequest();
        embeddingRequest.setModel(options.getModel());
        embeddingRequest.setInput(List.of(inputItem));
        embeddingRequest.setEncodingFormat(options.getEncodingFormat());
        embeddingRequest.setDimensions(options.getDimensions());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + options.getApiKey());
        
        HttpEntity<VolcengineEmbeddingRequest> entity = new HttpEntity<>(embeddingRequest, headers);
        
        log.debug("Calling Volcengine embedding API: {}", url);
        log.debug("Request: model={}, dimensions={}", options.getModel(), options.getDimensions());
        
        VolcengineEmbeddingResponse response = restTemplate.postForObject(url, entity, VolcengineEmbeddingResponse.class);
        
        if (response == null || response.getData() == null || response.getData().getEmbedding() == null) {
            throw new RuntimeException("Invalid response from Volcengine API");
        }
        
        VolcengineEmbeddingResponse.Data embeddingData = response.getData();
        List<Float> embeddingList = embeddingData.getEmbedding();
        
        float[] result = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            result[i] = embeddingList.get(i);
        }
        
        log.debug("Embedding generated successfully, dimension: {}", result.length);
        
        return result;
    }

    @Override
    public int dimensions() {
        return options.getDimensions();
    }
}
