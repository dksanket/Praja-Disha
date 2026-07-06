package gov.prajadisha.backend.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal note (visible only to organization staff).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedNote {
    private String userName;
    private String timestamp;
    private String text;
}
