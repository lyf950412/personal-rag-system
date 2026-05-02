package com.rag.service.storage;

import com.rag.dto.StsCredentialRequest;

import java.io.InputStream;

public interface ObjectStorageService {

    StsCredential getStsCredential(StsCredentialRequest request);

    InputStream download(String objectKey);

    void delete(String objectKey);
}
