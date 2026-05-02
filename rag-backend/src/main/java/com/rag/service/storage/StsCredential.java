package com.rag.service.storage;

import lombok.Data;

@Data
public class StsCredential {
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private String expiration;
    private String bucketName;

    public StsCredential() {}

    public StsCredential(String accessKeyId, String secretAccessKey, String sessionToken, String expiration, String bucketName) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
        this.expiration = expiration;
        this.bucketName = bucketName;

    }

}
