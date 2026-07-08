package gov.prajadisha.backend.task.dto;

import gov.prajadisha.backend.task.model.DetailedActivity;
import gov.prajadisha.backend.task.model.DetailedComment;
import gov.prajadisha.backend.task.model.DetailedNote;
import gov.prajadisha.backend.task.model.Task;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request/response DTOs for the Org-Admin task endpoints.
 */
public class TaskDtos {

    /** Row rendered in the central triage table (GET /api/dashboard/tasks). */
    public record DashboardTaskRow(
            String id,
            String title,
            String priority,
            List<Assignment> assignments,
            String dueDate,
            boolean dueDateCritical,
            String status,
            String statusType,
            String indicatorColorClass,
            String groupId) {}

    public record Assignment(String type, String value, String label) {
        public static Assignment dept(String label) {
            return new Assignment("dept", null, label);
        }
        public static Assignment avatar(String value, String label) {
            return new Assignment("avatar", value, label);
        }
    }

    public record DashboardStats(
            long awaitingAiReviewCount,
            long dueTodayCount,
            long myDeptCount) {}

    public record SubTask(
            String id,
            String parentId,
            String title,
            String role,
            String department,
            String icon,
            String status,
            String statusClass,
            String assignee,
            String timestamp
    ) {}

    /** Unified detail payload consumed by the ticket inspector page. */
    public record TaskDetailPayload(
            String id,
            String title,
            String priority,
            String groupId,
            String parentTaskId,
            String orgId,
            String globalStatus,
            String createdAt,
            String reportedBy,
            String reporterType,
            String description,
            String voiceUrl,
            String voiceDuration,
            String category,
            String language,
            Location location,
            String imageUrl,
            String mapUrl,
            List<SubTask> subTasks,
            List<DetailedComment> comments,
            List<DetailedNote> notes,
            List<DetailedActivity> activities,
            List<String> mediaUrls) {

        public record Location(String address, String lat, String lng) {}
    }

    public record CommentRequest(
            @NotBlank String text,
            boolean isOfficer,
            String userName) {}

    public record NoteRequest(
            @NotBlank String text,
            String userName) {}

    public record CreateSubTaskRequest(
            @NotBlank String title,
            String description,
            String priority,
            String category,
            String departmentId,
            String officerId) {}

    public record UpdateAssigneeRequest(
            String departmentId,
            String officerId) {}

    public record UpdateStatusRequest(
            @NotBlank String status,
            String remarks) {}
}
