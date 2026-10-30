package com.xiaoying.materialstudio.task;

import com.xiaoying.materialstudio.user.UserIdentityFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 受理后立即返回，制作在后台进行 */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskResponse create(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId,
                               @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(userId, request);
    }

    @GetMapping("/active")
    public List<TaskResponse> active(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId) {
        return taskService.listActive(userId);
    }

    @GetMapping("/{id}")
    public TaskResponse detail(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId,
                               @PathVariable("id") Long id) {
        return taskService.get(userId, id);
    }

    @GetMapping
    public List<TaskResponse> list(@RequestAttribute(UserIdentityFilter.CURRENT_USER_ID) Long userId,
                                   @RequestParam(name = "page", defaultValue = "1") int page,
                                   @RequestParam(name = "size", defaultValue = "20") int size) {
        return taskService.list(userId, page, size);
    }
}
