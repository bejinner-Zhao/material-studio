package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.common.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示用的模拟控制接口：调整失败阶段与每步耗时。
 * 不属于正式页面功能，可通过 app.simulator.dev-api-enabled=false 关闭。
 */
@RestController
@RequestMapping("/api/dev/simulator")
@ConditionalOnProperty(prefix = "app.simulator", name = "dev-api-enabled", havingValue = "true", matchIfMissing = true)
public class DevSimulatorController {

    private final SimulatorSettings settings;

    public DevSimulatorController(SimulatorSettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public SimulatorState current() {
        return new SimulatorState(
                settings.getFailStage() == null ? "NONE" : settings.getFailStage().name(),
                settings.getStageDelayMs()
        );
    }

    @PostMapping
    public SimulatorState update(@RequestBody SimulatorUpdateRequest request) {
        if (request.failStage() != null) {
            try {
                settings.setFailStage(SimulatorSettings.parseStage(request.failStage()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("failStage 只能是 NONE / MATERIAL_CHECK / GENERATING / FINALIZING");
            }
        }
        if (request.stageDelayMs() != null) {
            if (request.stageDelayMs() < 0) {
                throw new BadRequestException("stageDelayMs 不能为负数");
            }
            settings.setStageDelayMs(request.stageDelayMs());
        }
        return current();
    }

    public record SimulatorState(String failStage, long stageDelayMs) {
    }

    public record SimulatorUpdateRequest(String failStage, Long stageDelayMs) {
    }
}
