package com.promptvault.web;

import com.promptvault.common.ApiResponse;
import com.promptvault.common.BizException;
import com.promptvault.entity.RefImage;
import com.promptvault.service.ImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Path IMAGE_DIR = Paths.get("data", "images");

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ApiResponse<List<RefImage>> list(@RequestParam String refType,
                                            @RequestParam Long refId) {
        return ApiResponse.ok(imageService.list(refType, refId));
    }

    @PostMapping
    public ApiResponse<RefImage> upload(@RequestParam String refType,
                                        @RequestParam Long refId,
                                        @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(imageService.save(refType, refId, file));
    }

    /** 图片字节流：Content-Type 用记录的值，带缓存头 */
    @GetMapping("/{id}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable Long id) {
        RefImage image = imageService.get(id);
        if (image == null) {
            throw new BizException("图片不存在，可能已被删除");
        }
        Path path = IMAGE_DIR.resolve(image.getFilename());
        if (!Files.exists(path)) {
            throw new BizException("图片文件已丢失");
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BizException("读取图片失败：" + e.getMessage());
        }
        MediaType mediaType = image.getContentType() != null
                ? MediaType.parseMediaType(image.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(7, TimeUnit.DAYS).getHeaderValue())
                .contentType(mediaType)
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ApiResponse.ok();
    }
}
