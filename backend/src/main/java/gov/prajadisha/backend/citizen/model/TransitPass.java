package gov.prajadisha.backend.citizen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Reward coupon/pass redeemable by a citizen using transit points.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("transit_passes")
public class TransitPass {

    @Id
    private String id;

    @Indexed
    private String citizenUserName; // owner

    private String title;
    private int pointsCost;
    private String expiresAt;   // e.g. "16:30 PM"
    private String fareType;    // e.g. "Single Trip"
    private String qrCodeData;

    @JsonProperty("isActive")
    private boolean isActive;
}
