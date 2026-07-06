package gov.prajadisha.backend.task.event;

/**
 * Published after a citizen ticket is persisted, to trigger asynchronous auto-triage.
 */
public record TicketCreatedEvent(String taskId) {
}
