package gov.prajadisha.backend.org.dto;

import gov.prajadisha.backend.org.model.Officer;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer objects for officer authentication endpoints.
 */
public class OfficerDtos {

    public record LoginRequest(
            @NotBlank(message = "Identifier is required") String identifier
    ) {}

    public record LoginResponse(
            boolean exists,
            Officer profile,
            String token
    ) {}
}
