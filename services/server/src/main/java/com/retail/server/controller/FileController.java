package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FileController {

    private static final String UPLOAD_DIR_NAME = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "pdf", "doc", "docx", "xls", "xlsx"
    );

    /**
     * 通用文件上传接口。
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过 10MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension)) {
            String ext = extension.substring(1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BusinessException(400, "不支持的文件类型: " + ext);
            }
        }

        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path uploadDir = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR_NAME);

        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(newFileName).normalize();
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new BusinessException(500, "文件上传失败");
        }

        // 存相对路径，由前端 app.getImageUrl() 拼接完整 URL
        String fileUrl = "/uploads/" + newFileName;
        return Result.success("上传成功", fileUrl);
    }

    /**
     * 提取文件后缀并保留点号。
     */
    private String getExtension(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return StringUtils.hasText(extension) ? "." + extension : "";
    }
}
