package com.minimall.file.service;

import com.minimall.common.core.exception.BusinessException;
import com.minimall.file.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;


@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;
    @Autowired private MinioProperties props;

    /**
     * 上传文件
     * @param file    前端传来的文件
     * @param bizType 业务类型(product/avatar/...) 决定一级目录
     * @return 可访问的完整 URL
     */
    public String upload(MultipartFile file, String bizType) {
        // TODO 1: 校验 file 不为空 (空抛 BusinessException)
        if (file == null || file.isEmpty()) {
            throw new BusinessException(500, "文件为空");
        }
        // TODO 2: 构造 objectName, 格式: {bizType}/{今天日期}/{UUID}.{原扩展名}
        //         提示: file.getOriginalFilename() 拿原文件名,
        //               String.lastIndexOf(".") 取扩展名
        // TODO 2 ✅
        String fileName = file.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        String objectName = bizType + "/" + LocalDate.now() + "/" + UUID.randomUUID() + suffix;

        // ⭐ TODO 3 你写: 上传
         try (InputStream is = file.getInputStream()) {
              minioClient.putObject(
                 PutObjectArgs.builder()
                     .bucket(props.getBucket())
                     .object(objectName)
                     .stream(is, file.getSize(), -1)
                     .contentType(file.getContentType())
                     .build()
             );
         } catch (Exception e) {
              throw new BusinessException(500, "上传失败: " + e.getMessage());
          }

        // TODO 4: 返完整 URL
        return props.getPublicUrl() + "/" + objectName;
    }
    }