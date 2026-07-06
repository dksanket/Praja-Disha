package gov.prajadisha.backend.org.model;

import gov.prajadisha.backend.common.GeoPolygon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A municipal department. Supports nested hierarchical parent-child relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("departments")
public class Department {

    @Id
    private String id; // business key e.g. "DPT-001"

    @Indexed
    private String orgId;

    private String name;
    private String parentDepartmentId;   // null for top-level
    private String parentDepartmentName;
    private String headOfficerId;        // null if vacant
    private String headOfficerName;
    private String headOfficerAvatarUrl;
    private Integer officerCount;
    private Integer depth;               // 0 for root
    private String roleDescription;
    private DepartmentConstituency constituency;
    private String customPromptExtension;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentConstituency {
        private String name;
        private GeoPolygon coordinates;
    }
}
