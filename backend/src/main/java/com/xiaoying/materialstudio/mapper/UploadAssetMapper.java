package com.xiaoying.materialstudio.mapper;

import com.xiaoying.materialstudio.asset.UploadAsset;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface UploadAssetMapper {

    /** 插入后回填 id */
    int insert(UploadAsset asset);

    Optional<UploadAsset> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<UploadAsset> findByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
