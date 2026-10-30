package com.xiaoying.materialstudio.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskAssetMapper {

    int insert(@Param("taskId") Long taskId,
               @Param("assetId") Long assetId,
               @Param("sortOrder") int sortOrder);

    /** 按提交顺序返回任务的素材 id */
    List<Long> findAssetIds(@Param("taskId") Long taskId);
}
