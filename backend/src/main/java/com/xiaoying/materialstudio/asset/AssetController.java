package com.xiaoying.materialstudio.asset;

import com.xiaoying.materialstudio.user.UserIdentityFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/uploads")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId,
                                 @RequestParam("templateId") String templateId,
                                 @RequestParam("file") MultipartFile file) throws IOException {
        return assetService.upload(userId, templateId, file);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId,
                       @PathVariable("id") Long id) {
        assetService.delete(userId, id);
    }
}
