package com.techhub.controller;

import com.techhub.common.BusinessException;
import com.techhub.common.R;
import com.techhub.common.ResultCode;
import com.techhub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_OBJECT_TYPES = Set.of("user_avatar", "post_image");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostMapping("/upload")
    public R<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("objectType") String objectType) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 校验 objectType
        if (objectType == null || !ALLOWED_OBJECT_TYPES.contains(objectType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型，仅支持 user_avatar 或 post_image");
        }

        // 校验文件非空
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无法识别文件类型");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件格式，仅支持 JPG/PNG/GIF/WebP");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件大小不能超过 5MB");
        }

        // 使用 x-file-storage 上传
        org.dromara.x.file.storage.core.FileInfo fileInfo = fileStorageService.of(file)
                .setPath(objectType + "/")
                .upload();

        return R.ok(Map.of("url", fileInfo.getUrl()));
    }
}
