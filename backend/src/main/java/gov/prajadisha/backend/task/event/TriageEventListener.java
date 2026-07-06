package gov.prajadisha.backend.task.event;

import gov.prajadisha.backend.task.service.TaskService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs auto-triage asynchronously once a ticket has been created. Lives in a separate bean so
 * the {@link Async} proxy is honoured (self-invocation inside TaskService would bypass it).
 */
@Component
public class TriageEventListener {

    private final TaskService taskService;

    public TriageEventListener(TaskService taskService) {
        this.taskService = taskService;
    }

    @Async
    @EventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        taskService.autoTriage(event.taskId());
    }
}
