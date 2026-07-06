package gov.prajadisha.backend.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Audit history ledger entry for points earned or redeemed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("point_activities")
public class PointActivity {

    @Id
    private String id;

    @Indexed
    private String citizenUserName;

    private String title;   // "Pothole fixed on 5th Ave"
    private String source;  // "Verified by City Council"
    private String date;    // "Oct 24" or "Today"
    private int points;     // +earn / -spend

    private long createdAt; // epoch millis for ordering
}
