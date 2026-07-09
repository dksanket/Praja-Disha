package gov.prajadisha.backend.org.model;

import gov.prajadisha.backend.common.GeoPolygon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Primary municipal organization governing a constituency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("organizations")
public class Organization {

    @Id
    private String id;

    private String name;
    private String description;
    private long createdAt; // unix ms
    private OrgConstituency constituency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgConstituency {
        private String name;
        private GeoPolygon coordinates;
    }
}
