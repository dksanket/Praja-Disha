package gov.prajadisha.backend.task.controller;

import gov.prajadisha.backend.task.dto.TaskDtos.CommentRequest;
import gov.prajadisha.backend.task.dto.TaskDtos.NoteRequest;
import gov.prajadisha.backend.task.dto.TaskDtos.TaskDetailPayload;
import gov.prajadisha.backend.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Org-Admin task inspector endpoints (section 2.2 of the API contract).
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{id}/details")
    public TaskDetailPayload details(@PathVariable String id) {
        return taskService.detail(id);
    }

    @PostMapping("/{id}/comments")
    public TaskDetailPayload addComment(@PathVariable String id, @Valid @RequestBody CommentRequest req) {
        return taskService.addComment(id, req);
    }

    @PostMapping("/{id}/notes")
    public TaskDetailPayload addNote(@PathVariable String id, @Valid @RequestBody NoteRequest req) {
        return taskService.addNote(id, req);
    }
}
