package gov.prajadisha.backend.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sub-task assignment embedded in a {@link Task}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
    private String id;
    private String parentId;
    private String title;
    private String role;
    private String department;
    private String icon;
    private String status;
    private String statusClass;
    private String assignee;
    private String timestamp;
}
