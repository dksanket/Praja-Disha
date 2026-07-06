package gov.prajadisha.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class AiDtos {

    public record ChatRequest(@NotBlank String text) {}

    public record ChatResponse(
            String id,
            String sender,
            String text,
            long timestamp,
            List<String> suggestions) {}
}
