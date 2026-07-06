package gov.prajadisha.backend.org.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Admin settings: categories, priorities, statuses, and custom AI prompt overrides.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("organization_configs")
public class OrganizationConfig {

    @Id
    private String id;

    @Indexed(unique = true)
    private String orgId;

    private List<OrgCategory> categories;
    private List<String> priorities; // ["P0","P1","P2","P3"]
    private List<String> statuses;   // ["OPEN","RESOLVED","REJECTED"]
    private long updatedAt;
    private String customPromptExtension;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgCategory {
        private String id;          // "INFRASTRUCTURE"
        private String name;
        private String description; // AI classification matching target
    }
}
