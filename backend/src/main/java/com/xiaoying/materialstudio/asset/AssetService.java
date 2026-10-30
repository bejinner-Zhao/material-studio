package com.xiaoying.materialstudio.asset;

import com.xiaoying.materialstudio.common.BadRequestException;
import com.xiaoying.materialstudio.common.NotFoundException;
import com.xiaoying.materialstudio.config.StorageProperties;
import com.xiaoying.materialstudio.mapper.TemplateMapper;
import com.xiaoying.materialstudio.mapper.UploadAssetMapper;
import com.xiaoying.materialstudio.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private static final Map<String, String> EXTENSION_BY_MIME = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final UploadAssetMapper uploadAssetMapper;
    private final TemplateMapper templateMapper;
    private final StorageProperties storage;

    public AssetService(UploadAssetMapper uploadAssetMapper,
                        TemplateMapper templateMapper,
                        StorageProperties storage) {
        this.uploadAssetMapper = uploadAssetMapper;
        this.templateMapper = templateMapper;
        this.storage = storage;
    }

    public UploadResponse upload(Long userId, String templateId, MultipartFile file) throws IOException {
        Template template = templateMapper.findById(templateId)
                .orElseThrow(() -> new NotFoundException("模板不存在：" + templateId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("请选择要上传的图片");
        }
        if (file.getSize() > template.getMaxFileSizeBytes()) {
            throw new BadRequestException("图片大小超出上限（最多 "
                    + template.getMaxFileSizeBytes() / 1024 / 1024 + " MB）");
        }

        String mimeType = ImageTypeDetector.detect(readHeader(file));
        if (mimeType == null) {
            throw new BadRequestException("无法识别的图片格式，仅支持 JPEG / PNG / WebP");
        }
        List<String> allowed = template.acceptedMimeTypeList();
        if (!allowed.contains(mimeType)) {
            throw new BadRequestException("该模板仅支持 " + String.join(" / ", allowed));
        }

        String fileName = UUID.randomUUID() + "." + EXTENSION_BY_MIME.get(mimeType);
        Path directory = storage.rootPath().resolve("uploads").resolve(String.valueOf(userId));
        Files.createDirectories(directory);
        file.transferTo(directory.resolve(fileName));

        String storedPath = "uploads/" + userId + "/" + fileName;
        UploadAsset asset = new UploadAsset();
        asset.setUserId(userId);
        asset.setTemplateId(templateId);
        asset.setOriginalName(sanitizeFileName(file.getOriginalFilename()));
        asset.setStoredPath(storedPath);
        asset.setMimeType(mimeType);
        asset.setSizeBytes(file.getSize());
        uploadAssetMapper.insert(asset);

        return new UploadResponse(asset.getId(), "/files/" + storedPath, mimeType,
                asset.getSizeBytes(), asset.getOriginalName());
    }

    @Transactional
    public void delete(Long userId, Long assetId) {
        UploadAsset asset = uploadAssetMapper.findByIdAndUserId(assetId, userId)
                .orElseThrow(() -> new NotFoundException("素材不存在：" + assetId));
        uploadAssetMapper.deleteByIdAndUserId(assetId, userId);

        Path target = storage.rootPath().resolve(asset.getStoredPath());
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            // 记录已删除，文件残留不影响用户；仅记录以便后续清理
            log.warn("删除素材文件失败: {}", target, ex);
        }
    }

    private static byte[] readHeader(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(12);
        }
    }

    /** 原始文件名只用于展示，去掉目录部分并截断，避免异常长值写入列 */
    private static String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "image";
        }
        String name = raw.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
