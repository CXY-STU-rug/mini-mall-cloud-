package com.minimall.file.controller;

import com.minimall.common.core.domain.Result;
import com.minimall.file.config.MinioProperties;
import com.minimall.file.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private MinioService minioService;
    @Autowired private MinioProperties props;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "product") String bizType) {
        // TODO: 调 service.upload, 把 url 包成 Map 返回
        //       Map.of("url", url, "objectName", objectName) 风格
        String fileUrl = minioService.upload(file, bizType);
        String objectName = fileUrl.replace(props.getPublicUrl() + "/", "");
        return Result.success(Map.of("url", fileUrl, "objectName", objectName));
    }
}
