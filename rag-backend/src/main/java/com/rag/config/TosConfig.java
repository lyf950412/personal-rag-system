package com.rag.config;

import cn.hutool.core.lang.UUID;
import com.rag.service.storage.StsCredential;
import com.volcengine.model.request.AssumeRoleRequest;
import com.volcengine.model.response.AssumeRoleResponse;
import com.volcengine.service.sts.ISTSService;
import com.volcengine.service.sts.impl.STSServiceImpl;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.credential.StaticCredentialsProvider;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.tos")
public class TosConfig {

    private static final Logger log = LoggerFactory.getLogger(TosConfig.class);

    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String roleArn;
    private Long accountId=2106760570L;

    @Bean
    public TOSV2 tosClient() {
        com.volcengine.tos.credential.CredentialsProvider credentialsProvider = 
            new StaticCredentialsProvider(accessKey, secretKey);
        return new TOSV2ClientBuilder().build(region,endpoint, credentialsProvider);
    }

    @Bean
    public String tosBucketName() {
        return bucketName;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public StsCredential getStsCredential(Integer durationSeconds, String objectKey, Long fileSize) {
        log.info("获取STS临时凭证 - roleArn: {}, duration: {}秒, objectKey: {}, fileSize: {}", 
                roleArn, durationSeconds, objectKey, fileSize);
        log.info("当前配置 - accessKey: {}, region: {}, bucketName: {}", 
                accessKey != null ? accessKey.substring(0, Math.min(8, accessKey.length())) + "***" : "null",
                region, bucketName);
        
        if (roleArn == null || roleArn.isEmpty()) {
            throw new RuntimeException("roleArn 未配置，请检查环境变量 TOS_ROLE_ARN 或配置文件");
        }
        
        try {
            ISTSService stsService = STSServiceImpl.getInstance();
            stsService.setAccessKey(accessKey);
            stsService.setSecretKey(secretKey);
            stsService.setRegion(region);

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleSessionName("tos_upload_session");
            request.setDurationSeconds(durationSeconds);
            request.setRoleTrn(roleArn);
            
            if (objectKey != null && !objectKey.isEmpty()) {
                String policy = generateUploadPolicy(objectKey);
                request.setPolicy(policy);
                log.info("设置STS Policy策略: {}", policy);
            }

            log.info("调用 STS AssumeRole - RoleTrn: {}, RoleSessionName: {}", 
                    request.getRoleTrn(), request.getRoleSessionName());

            AssumeRoleResponse resp = stsService.assumeRole(request);
            AssumeRoleResponse.ResultBean result = resp.getResult();
            AssumeRoleResponse.Credentials credentials = result.getCredentials();

            StsCredential stsCredential = new StsCredential(
                    credentials.getAccessKeyId(),
                    credentials.getSecretAccessKey() ,
                    credentials.getSessionToken(),
                    credentials.getExpiredTime(),
                    bucketName
            );
            
            log.info("STS凭证获取成功 - accessKeyId: {}, expiredTime: {}", 
                    stsCredential.getAccessKeyId(), stsCredential.getExpiration());
            
            return stsCredential;
            
        } catch (Exception e) {
            log.error("获取STS凭证失败 - RoleTrn: {}", roleArn, e);
            throw new RuntimeException("获取STS临时凭证失败: " + e.getMessage(), e);
        }
    }
    
    public String generateObjectKey(String fileName, String filePath) {
        String storedFilename = UUID.randomUUID().toString() + "_" + sanitizeFilename(fileName);
        String cleanPath = filePath != null ? filePath.replaceAll("^/+", "") : "";
        if (cleanPath.isEmpty()) {
            return "documents/" + storedFilename;
        }
        return cleanPath + "/" + storedFilename;
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    private String generateUploadPolicy(String objectKey) {
        String directory = objectKey.contains("/") ? objectKey.substring(0, objectKey.lastIndexOf("/")) : "";
        StringBuilder policy = new StringBuilder();
        policy.append("{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"tos:PutObject\"],\"Resource\":[\"trn:tos:::");
        policy.append(bucketName);
        policy.append("/");
        policy.append(directory.isEmpty() ? "*" : directory + "/*");
        policy.append("\"]}]}");
        
        log.info("生成上传Policy - objectKey: {}, directory: {}, policy: {}", objectKey, directory, policy.toString());

        return policy.toString();
    }
    
    private String calculateSignature(String date, String region, String payload, String signedHeaders, String stringToSign) throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + secretKey).getBytes(), date);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, "iam");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        byte[] signature = hmacSha256(kSigning, stringToSign);
        return bytesToHex(signature);
    }
    
    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes());
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
