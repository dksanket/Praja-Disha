package gov.prajadisha.backend.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The currently logged-in citizen user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("citizens")
public class CitizenProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String name;
    private String email;

    @Indexed(unique = true, sparse = true)
    private String phone;

    private int points;
    private String tier;      // "Bronze Citizen", "Silver Citizen", "Gold Citizen"
    private String language;  // "en", "kn", "hi"
}
