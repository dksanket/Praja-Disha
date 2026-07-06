package gov.prajadisha.backend.task.controller;

import gov.prajadisha.backend.task.dto.TaskDtos.DashboardStats;
import gov.prajadisha.backend.task.dto.TaskDtos.DashboardTaskRow;
import gov.prajadisha.backend.task.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TaskService taskService;

    public DashboardController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    public List<DashboardTaskRow> tasks(
            @RequestParam(required = false) String statusType,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return taskService.dashboardTasks(statusType, priority, page, pageSize);
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return taskService.stats();
    }
}
