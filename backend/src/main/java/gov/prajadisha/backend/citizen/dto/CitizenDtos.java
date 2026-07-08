package gov.prajadisha.backend.citizen.dto;

import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.PointActivity;
import gov.prajadisha.backend.citizen.model.TransitPass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request/response DTOs for the Citizen App endpoints.
 */
public class CitizenDtos {

    public record LoginRequest(@NotBlank String identifier) {}

    /** Login/register response including JWT token for subsequent API calls. */
    public record LoginResponse(boolean exists, CitizenProfile profile, String token) {}

    public record RegisterRequest(
            @NotBlank String name,
            String phone,
            String email,
            String language) {}

    /** A lightweight row representation of a citizen's ticket for the Track list view. */
    public record TicketRow(
            String id,
            String category,
            String title,
            String description,
            String date,
            String status,
            String lastUpdate,
            String location,
            String imageUrl) {}

    public record CreateTicketRequest(
            String title,
            String description,
            String location,
            Double latitude,
            Double longitude,
            String imageUrl,
            String voiceUrl,
            String voiceDuration,
            java.util.List<String> mediaUrls,
            String language) {}

    public record CreateTicketResponse(String id) {}

    public record FeedbackRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            String comment) {}

    public record FeedbackResponse(
            String message,
            int awardedPoints,
            int updatedPoints,
            String tier) {}

    public record ReopenResponse(String id, String status, String message) {}

    public record LanguageRequest(@NotBlank String language) {}

    public record LanguageResponse(boolean success, String language) {}

    public record RedeemRequest(
            @NotNull @Min(0) Integer pointsCost,
            @NotBlank String title) {}

    public record RedeemResponse(
            boolean success,
            int updatedPoints,
            TransitPass pass) {}

    /** Bundles passes and point activities for the wallet page. */
    public record WalletData(
            CitizenProfile profile,
            List<TransitPass> passes,
            List<PointActivity> activities) {}
}
