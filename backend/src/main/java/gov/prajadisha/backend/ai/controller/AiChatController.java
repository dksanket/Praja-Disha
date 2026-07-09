package gov.prajadisha.backend.ai.controller;

import gov.prajadisha.backend.ai.dto.AiDtos.ChatRequest;
import gov.prajadisha.backend.ai.dto.AiDtos.ChatResponse;
import gov.prajadisha.backend.ai.service.AiChatService;
import gov.prajadisha.backend.common.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final JwtService jwtService;

    public AiChatController(AiChatService aiChatService, JwtService jwtService) {
        this.aiChatService = aiChatService;
        this.jwtService = jwtService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, HttpServletRequest request) {
        String officerUserName = resolveOfficer(request);
        return aiChatService.reply(req.text(), officerUserName);
    }

    @GetMapping("/chat/history")
    public List<ChatResponse> getHistory(HttpServletRequest request) {
        String officerUserName = resolveOfficer(request);
        return aiChatService.getHistory(officerUserName);
    }

    private String resolveOfficer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isValid(token)) {
                return jwtService.extractUsername(token);
            }
        }
        // Fallback for dev/test without a token
        String headerUsername = request.getHeader("X-Officer-Username");
        if (headerUsername != null && !headerUsername.isBlank()) {
            return headerUsername;
        }
        return "aarav_sharma"; // New Delhi MP office admin officer fallback
    }
}
