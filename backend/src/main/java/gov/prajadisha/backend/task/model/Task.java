package gov.prajadisha.backend.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.prajadisha.backend.common.GeoPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Core civic issue ticket. Comments / notes / activities / subtasks are embedded so the
 * detail payload can be assembled from a single document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("tasks")
public class Task {

    @Id
    private String id; // "PD-8821"

    @Indexed
    private String groupId;

    private String parentTaskId;

    @Indexed
    private String orgId;

    @Indexed
    private String citizenUserName;

    private String title;
    private String description;
    private String voiceUrl;
    private String imageUrl;

    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    private String language;
    private TaskLocation location;
    private String category;
    private String priority;      // P0..P3
    private String globalStatus;  // "Submitted", "AI-Assigned", "In Progress", "Resolved"...

    @JsonProperty("isReviewed")
    private boolean isReviewed;
    private long dueDate;         // unix ms
    private long createdAt;       // unix ms

    private List<Double> descriptionEmbedding;

    // Embedded detail collections
    @Builder.Default
    private List<DetailedComment> comments = new ArrayList<>();
    @Builder.Default
    private List<DetailedNote> notes = new ArrayList<>();
    @Builder.Default
    private List<DetailedActivity> activities = new ArrayList<>();

    // Fields used by the dashboard row projection
    private String voiceDuration;
    private String reporterType; // "Citizen", "Officer", ...

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskLocation {
        private String address;
        private GeoPoint geo;
    }
}
