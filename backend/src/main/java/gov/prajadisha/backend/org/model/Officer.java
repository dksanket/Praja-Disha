package gov.prajadisha.backend.org.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Staff/officer assigned to triage and resolve issues. Can belong to multiple departments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("officers")
public class Officer {

    @Id
    private String id;

    private List<String> orgIds;

    @Indexed(unique = true)
    private String officerUserName; // "kiran_kumar"

    private String name;
    private String email;
    private String phone;
    private List<String> departmentIds;

    @JsonProperty("isActive")
    private boolean isActive;
    private List<String> managerUserNames;
    private long createdAt; // unix ms
}
