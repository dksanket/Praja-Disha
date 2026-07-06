package gov.prajadisha.backend.org.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Manager-reportee connection used for escalation paths.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("reporting_hierarchies")
public class ReportingHierarchy {

    @Id
    private String id;

    @Indexed
    private String orgId;

    private String managerUserName;  // -> Officer.officerUserName
    private String reporteeUserName; // -> Officer.officerUserName
    private long assignedAt;         // unix ms
}
