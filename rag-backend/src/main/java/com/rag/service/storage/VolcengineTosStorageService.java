package com.rag.service.storage;

import com.rag.config.TosConfig;
import com.rag.dto.StsCredentialRequest;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.model.object.GetObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class VolcengineTosStorageService implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(VolcengineTosStorageService.class);

    private final TOSV2 tosClient;
    private final String bucketName;
    private final TosConfig tosConfig;

    public VolcengineTosStorageService(TOSV2 tosClient, String bucketName, TosConfig tosConfig) {
        this.tosClient = tosClient;
        this.bucketName = bucketName;
        this.tosConfig = tosConfig;
    }

    @Override
    public StsCredential getStsCredential(StsCredentialRequest request) {
        String objectKey = tosConfig.generateObjectKey(request.getFileName(), request.getFilePath());
        return tosConfig.getStsCredential(3600, objectKey, request.getFileSize());
    }

    @Override
    public InputStream download(String objectKey) {
        log.info("从TOS下载文件 - bucket: {}, key: {}", bucketName, objectKey);
        try {
            GetObjectOutput output = tosClient.getObject(bucketName, objectKey);
            log.info("文件下载成功 - key: {}", objectKey);
            return output.getContent();
        } catch (TosServerException e) {
            log.error("TOS服务器异常 - key: {}, code: {}, message: {}", objectKey, e.getCode(), e.getMessage());
            throw new RuntimeException("TOS下载失败: " + e.getMessage(), e);
        } catch (TosClientException e) {
            log.error("TOS客户端异常 - key: {}, message: {}", objectKey, e.getMessage());
            throw new RuntimeException("TOS下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        log.info("从TOS删除文件 - bucket: {}, key: {}", bucketName, objectKey);
        try {
            tosClient.deleteObject(bucketName, objectKey);
            log.info("文件删除成功 - key: {}", objectKey);
        } catch (TosServerException e) {
            log.error("TOS服务器异常 - key: {}, code: {}, message: {}", objectKey, e.getCode(), e.getMessage());
            throw new RuntimeException("TOS删除失败: " + e.getMessage(), e);
        } catch (TosClientException e) {
            log.error("TOS客户端异常 - key: {}, message: {}", objectKey, e.getMessage());
            throw new RuntimeException("TOS删除失败: " + e.getMessage(), e);
        }
    }
}
