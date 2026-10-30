package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.mapper.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 从数据库取 PENDING 任务并交给线程池执行。
 * 取之前先看线程池是否有空闲线程，避免把任务标成 RUNNING 却排在队列里等，
 * 这样前端看到的状态始终与真实执行情况一致。
 */
@Component
public class TaskDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TaskDispatcher.class);
    private static final String INTERRUPTED_MESSAGE = "服务重启，任务执行中断";

    private final TaskMapper taskMapper;
    private final TaskWorker taskWorker;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final SimulatorProperties simulatorProperties;

    /** 启动自检只做一次；失败（例如数据库尚未就绪）时保持 false，下一次扫描重试 */
    private boolean recoveryDone = false;

    public TaskDispatcher(TaskMapper taskMapper,
                          TaskWorker taskWorker,
                          @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor,
                          SimulatorProperties simulatorProperties) {
        this.taskMapper = taskMapper;
        this.taskWorker = taskWorker;
        this.taskExecutor = taskExecutor;
        this.simulatorProperties = simulatorProperties;
    }

    @Scheduled(fixedDelayString = "${app.simulator.poll-interval-ms:300}")
    public void dispatch() {
        recoverInterruptedTasks();

        int freeSlots = simulatorProperties.getMaxConcurrent() - taskExecutor.getActiveCount();
        if (freeSlots <= 0) {
            return;
        }
        List<Long> pendingIds = taskMapper.findPendingIds(freeSlots);
        for (Long taskId : pendingIds) {
            if (taskMapper.claim(taskId) == 1) {
                taskExecutor.execute(() -> taskWorker.run(taskId));
            }
        }
    }

    /**
     * 上一个进程留下的 RUNNING 任务已经没有线程在跑了，标记为失败。
     * 放在调度线程的第一次扫描里执行：此时尚未认领任何任务，不会误伤本进程刚抢到的任务。
     */
    private void recoverInterruptedTasks() {
        if (recoveryDone) {
            return;
        }
        int recovered = taskMapper.failInterruptedTasks(INTERRUPTED_MESSAGE);
        recoveryDone = true;
        if (recovered > 0) {
            log.warn("启动自检：{} 个中断任务已标记为失败", recovered);
        }
    }
}
