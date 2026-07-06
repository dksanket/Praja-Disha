package gov.prajadisha.backend.citizen.dto;

import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.TransitPass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request/response DTOs for the Citizen App endpoints.
 */
public class CitizenDtos {

    public record LoginRequest(@NotBlank String identifier) {}

    public record LoginResponse(boolean exists, CitizenProfile profile) {}

    public record RegisterRequest(
            @NotBlank String name,
            String phone,
            String email,
            String language) {}

    public record CreateTicketRequest(
            @NotBlank String title,
            String description,
            String location,
            String imageUrl) {}

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
}
