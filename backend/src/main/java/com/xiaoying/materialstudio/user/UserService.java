package com.xiaoying.materialstudio.user;

import com.xiaoying.materialstudio.mapper.AppUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserMapper appUserMapper;

    public UserService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    /**
     * 按 Cookie 中的 publicId 取用户，不存在则创建。
     * 同一 publicId 并发首次访问时会撞唯一索引，此处退化为再查一次。
     */
    public AppUser resolveOrCreate(String publicId) {
        return appUserMapper.findByPublicId(publicId)
                .orElseGet(() -> create(publicId));
    }

    public AppUser getById(Long id) {
        return appUserMapper.findById(id)
                .orElseThrow(() -> new IllegalStateException("用户不存在: " + id));
    }

    private AppUser create(String publicId) {
        AppUser user = new AppUser();
        user.setPublicId(publicId);
        user.setNickname("访客-" + publicId.substring(0, Math.min(4, publicId.length())));
        try {
            appUserMapper.insert(user);
            return user;
        } catch (DuplicateKeyException ex) {
            return appUserMapper.findByPublicId(publicId)
                    .orElseThrow(() -> ex);
        }
    }
}
