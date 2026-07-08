package gov.prajadisha.backend.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Assignment mapping a Task to a specific Department and optionally an Officer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("task_assignments")
public class TaskAssignment {

    @Id
    private String id;

    @Indexed
    private String taskId;

    @Indexed
    private String departmentId;

    private String officerId;
    private String status; // "PENDING", "IN_PROGRESS", "RESOLVED", etc.
    private long assignedAt;
}
