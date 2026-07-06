package gov.prajadisha.backend.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Timeline audit entry summarizing a historical transition of a ticket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedActivity {
    private String timestamp;
    private String action;      // "AI_ASSIGNED", "COMMENT_ADDED", "STATUS_CHANGED", "DELEGATED"
    private String performedBy; // officer name or "system_ai"
    private String remarks;
}
