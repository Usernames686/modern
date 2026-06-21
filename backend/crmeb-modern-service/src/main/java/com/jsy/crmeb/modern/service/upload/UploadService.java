package com.jsy.crmeb.modern.service.upload;

import com.jsy.crmeb.modern.common.config.CrmebRuntimeProperties;
import com.jsy.crmeb.modern.service.upload.dto.FileResultResponse;
import com.jsy.crmeb.modern.service.upload.mapper.UploadMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadService {
    private static final String IMAGE_EXT_KEY = "image_ext_str";
    private static final String FILE_EXT_KEY = "file_ext_str";
    private static final String IMAGE_MAX_SIZE_KEY = "image_max_size";
    private static final String FILE_MAX_SIZE_KEY = "file_max_size";
    private static final String DEFAULT_IMAGE_EXT = "jpg,jpeg,gif,png,bmp";
    private static final String DEFAULT_FILE_EXT = "zip,doc,docx,xls,xlsx,pdf,mp3,wma,wav,amr,mp4,pem,p12";
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final CrmebRuntimeProperties runtimeProperties;
    private final UploadMapper uploadMapper;

    public UploadService(CrmebRuntimeProperties runtimeProperties, UploadMapper uploadMapper) {
        this.runtimeProperties = runtimeProperties;
        this.uploadMapper = uploadMapper;
    }

    @Transactional
    public FileResultResponse imageUpload(
            String originalFilename,
            String contentType,
            long fileSize,
            InputStream inputStream,
            String model,
            Integer pid) {
        return upload(originalFilename, contentType, fileSize, inputStream, model, pid, IMAGE_EXT_KEY, IMAGE_MAX_SIZE_KEY, DEFAULT_IMAGE_EXT, 10);
    }

    @Transactional
    public FileResultResponse fileUpload(
            String originalFilename,
            String contentType,
            long fileSize,
            InputStream inputStream,
            String model,
            Integer pid) {
        return upload(originalFilename, contentType, fileSize, inputStream, model, pid, FILE_EXT_KEY, FILE_MAX_SIZE_KEY, DEFAULT_FILE_EXT, 20);
    }

    private FileResultResponse upload(
            String originalFilename,
            String contentType,
            long fileSize,
            InputStream inputStream,
            String model,
            Integer pid,
            String extConfigKey,
            String sizeConfigKey,
            String defaultExts,
            int defaultMaxSizeMb) {
        if (inputStream == null || fileSize <= 0) {
            throw new IllegalArgumentException("上载的文件对象不存在...");
        }
        String originalName = cleanOriginalName(originalFilename);
        String extName = resolveExtName(originalName, contentType);
        validateExt(extName, configValueByKey(extConfigKey, defaultExts));
        validateSize(fileSize, parsePositiveInt(configValueByKey(sizeConfigKey, null), defaultMaxSizeMb));

        if (originalName.length() > 99) {
            originalName = originalName.substring(0, Math.min(90, originalName.length())) + "." + extName;
        }
        String modelPath = sanitizePathPart(model, "product");
        String webDir = "crmebimage/public/" + modelPath + "/" + LocalDate.now().format(DATE_PATH) + "/";
        String newFileName = UUID.randomUUID().toString().replace("-", "") + randomSuffix() + "." + extName;
        Path root = Path.of(valueOrFallback(runtimeProperties.getImagePath(), "./upload/")).toAbsolutePath().normalize();
        Path target = root.resolve(webDir).resolve(newFileName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("文件路径非法");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("文件上传 IO异常");
        }

        String url = webDir + newFileName;
        String type = normalizeType(contentType, extName);
        uploadMapper.insertAttachment(originalName, url, Long.toString(fileSize), type, pid == null ? 0 : pid);
        return new FileResultResponse(originalName, extName, fileSize, url, type);
    }

    private void validateExt(String extName, String extConfig) {
        Set<String> allowed = Arrays.stream(extConfig.split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .collect(Collectors.toSet());
        if (!allowed.contains(extName)) {
            throw new IllegalArgumentException("上载文件类型只能为：" + extConfig);
        }
    }

    private void validateSize(long sizeBytes, int maxSizeMb) {
        float sizeMb = (float) sizeBytes / 1024 / 1024;
        if (sizeMb > maxSizeMb) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "最大允许上传 %d MB文件，当前文件大小为 %.2f MB", maxSizeMb, sizeMb));
        }
    }

    private String configValueByKey(String key, String fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        String value = uploadMapper.findConfigValue(key);
        return valueOrFallback(value, fallback);
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String cleanOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "upload";
        }
        String normalized = originalName.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String resolveExtName(String fileName, String contentType) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (contentType != null && contentType.contains("/")) {
            return contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("文件类型未定义，无法上传...");
    }

    private String normalizeType(String contentType, String extName) {
        if (contentType == null || contentType.isBlank()) {
            return extName;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (lower.startsWith("image/")) {
            return lower.substring("image/".length());
        }
        if (lower.startsWith("file/")) {
            return lower.substring("file/".length());
        }
        return extName;
    }

    private String sanitizePathPart(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
