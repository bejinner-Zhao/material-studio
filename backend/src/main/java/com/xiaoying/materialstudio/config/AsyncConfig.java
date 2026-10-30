package com.xiaoying.materialstudio.config;

import com.xiaoying.materialstudio.task.SimulatorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class AsyncConfig {

    /**
     * 任务执行线程池。并发度固定为 max-concurrent，多余的 PENDING 任务留在库里排队，
     * 由调度器在有空闲线程时才取走，保证 status 与真实执行状态一致。
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor(SimulatorProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getMaxConcurrent());
        executor.setMaxPoolSize(properties.getMaxConcurrent());
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("task-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
