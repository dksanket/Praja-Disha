package gov.prajadisha.backend.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public-facing comment (visible to citizens).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedComment {
    private String userName;
    private String initials;
    private String timestamp;
    private String text;

    @JsonProperty("isSelf")
    private boolean isSelf;
    private String imageUrl;
    private String department;
}
