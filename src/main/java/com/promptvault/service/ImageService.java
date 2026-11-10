package com.promptvault.service;

import com.promptvault.common.BizException;
import com.promptvault.entity.RefImage;
import com.promptvault.mapper.PresetMapper;
import com.promptvault.mapper.RefImageMapper;
import com.promptvault.mapper.TagGroupMapper;
import com.promptvault.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 参考图片：文件存 ./data/images/（相对工作目录），记录落 ref_image 表。
 * refType 仅允许 tag / group / preset，保存前校验目标存在。
 */
@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    /** 图片存储目录（相对工作目录） */
    private static final Path IMAGE_DIR = Paths.get("data", "images");

    /** 每个目标最多挂的参考图数量 */
    private static final int MAX_PER_REF = 20;

    private static final List<String> REF_TYPES = List.of("tag", "group", "preset");

    /** content-type 缺扩展名时的推断表 */
    private static final Map<String, String> EXT_BY_CONTENT_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "image/bmp", ".bmp",
            "image/svg+xml", ".svg"
    );

    private final RefImageMapper imageMapper;
    private final TagMapper tagMapper;
    private final TagGroupMapper groupMapper;
    private final PresetMapper presetMapper;

    public ImageService(RefImageMapper imageMapper,
                        TagMapper tagMapper,
                        TagGroupMapper groupMapper,
                        PresetMapper presetMapper) {
        this.imageMapper = imageMapper;
        this.tagMapper = tagMapper;
        this.groupMapper = groupMapper;
        this.presetMapper = presetMapper;
        try {
            Files.createDirectories(IMAGE_DIR);
        } catch (IOException e) {
            log.warn("创建图片目录失败：{}", e.getMessage());
        }
    }

    public RefImage save(String refType, Long refId, MultipartFile file) {
        requireRef(refType, refId);
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("只能上传图片文件");
        }
        if (imageMapper.countByRef(refType, refId) >= MAX_PER_REF) {
            throw new BizException("每个目标最多上传 " + MAX_PER_REF + " 张参考图");
        }

        String origName = file.getOriginalFilename();
        String filename = UUID.randomUUID().toString().replace("-", "") + extensionOf(origName, contentType);
        try {
            Files.createDirectories(IMAGE_DIR);
            file.transferTo(IMAGE_DIR.resolve(filename));
        } catch (IOException e) {
            throw new BizException("图片保存失败：" + e.getMessage());
        }

        RefImage image = new RefImage();
        image.setRefType(refType);
        image.setRefId(refId);
        image.setFilename(filename);
        image.setOrigName(origName);
        image.setContentType(contentType);
        image.setSize(file.getSize());
        imageMapper.insert(image);
        return imageMapper.findById(image.getId());
    }

    public List<RefImage> list(String refType, Long refId) {
        return imageMapper.listByRef(refType, refId);
    }

    public RefImage get(Long id) {
        return imageMapper.findById(id);
    }

    public void delete(Long id) {
        RefImage image = imageMapper.findById(id);
        if (image == null) {
            throw new BizException("图片不存在，可能已被删除");
        }
        imageMapper.delete(id);
        deleteFileQuietly(image.getFilename());
    }

    /** 级联删除：先查列表逐个删文件，再删记录 */
    public void deleteByRef(String refType, Long refId) {
        List<RefImage> images = imageMapper.listByRef(refType, refId);
        if (images.isEmpty()) {
            return;
        }
        for (RefImage image : images) {
            deleteFileQuietly(image.getFilename());
        }
        imageMapper.deleteByRef(refType, refId);
    }

    private void requireRef(String refType, Long refId) {
        if (refType == null || !REF_TYPES.contains(refType)) {
            throw new BizException("未知的参考图类型：" + refType);
        }
        if (refId == null) {
            throw new BizException("缺少参考图目标 id");
        }
        boolean exists = switch (refType) {
            case "tag" -> tagMapper.findById(refId) != null;
            case "group" -> groupMapper.findById(refId) != null;
            case "preset" -> presetMapper.findById(refId) != null;
            default -> false;
        };
        if (!exists) {
            throw new BizException("参考图目标不存在，可能已被删除");
        }
    }

    private String extensionOf(String origName, String contentType) {
        if (origName != null) {
            int dot = origName.lastIndexOf('.');
            if (dot >= 0 && dot < origName.length() - 1) {
                String ext = origName.substring(dot).toLowerCase();
                if (ext.length() <= 6) {
                    return ext;
                }
            }
        }
        return EXT_BY_CONTENT_TYPE.getOrDefault(contentType, ".png");
    }

    private void deleteFileQuietly(String filename) {
        try {
            Files.deleteIfExists(IMAGE_DIR.resolve(filename));
        } catch (IOException e) {
            log.warn("删除图片文件失败：{}，{}", filename, e.getMessage());
        }
    }
}
