package com.xiaoying.materialstudio.mapper;

import com.xiaoying.materialstudio.task.CreationTask;
import com.xiaoying.materialstudio.task.TaskStage;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface TaskMapper {

    /** 插入后回填 id */
    int insert(CreationTask task);

    Optional<CreationTask> findById(@Param("id") Long id);

    Optional<CreationTask> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<CreationTask> findByUserId(@Param("userId") Long userId,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    List<CreationTask> findActiveByUserId(@Param("userId") Long userId);

    List<Long> findPendingIds(@Param("limit") int limit);

    /** 原子抢单：只有 PENDING 才能被置为 RUNNING，返回受影响行数 */
    int claim(@Param("id") Long id);

    int updateStage(@Param("id") Long id, @Param("stage") TaskStage stage);

    int markSucceeded(@Param("id") Long id, @Param("resultUrl") String resultUrl);

    int markFailed(@Param("id") Long id, @Param("stage") TaskStage stage, @Param("message") String message);

    /** 服务启动时把上次进程遗留的 RUNNING 任务标记为失败，避免永远卡在处理中 */
    int failInterruptedTasks(@Param("message") String message);
}
