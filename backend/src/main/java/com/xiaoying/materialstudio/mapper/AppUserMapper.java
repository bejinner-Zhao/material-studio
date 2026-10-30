package com.xiaoying.materialstudio.mapper;

import com.xiaoying.materialstudio.user.AppUser;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface AppUserMapper {

    Optional<AppUser> findByPublicId(@Param("publicId") String publicId);

    Optional<AppUser> findById(@Param("id") Long id);

    /** 插入后回填 id */
    int insert(AppUser user);
}
