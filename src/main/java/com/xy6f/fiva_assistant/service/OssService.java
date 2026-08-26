package com.xy6f.fiva_assistant.service;

import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OssService {

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.domain}")
    private String domain;

    /**
     * 上传文件到 OSS，返回公网访问 URL
     */
    public String uploadFile(MultipartFile file, String dir) {
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String objectKey = dir + "/" + dateStr + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

            InputStream inputStream = file.getInputStream();
            ossClient.putObject(bucketName, objectKey, inputStream);
            inputStream.close();

            String url = domain + "/" + objectKey;
            log.info("OSS upload success: {}", url);
            return url;
        } catch (Exception e) {
            log.error("OSS upload failed", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除 OSS 文件
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || !fileUrl.startsWith(domain)) return;
            String objectKey = fileUrl.substring(domain.length() + 1);
            ossClient.deleteObject(bucketName, objectKey);
            log.info("OSS delete success: {}", objectKey);
        } catch (Exception e) {
            log.error("OSS delete failed", e);
        }
    }
}
