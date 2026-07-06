package gov.prajadisha.backend.ai.controller;

import gov.prajadisha.backend.ai.dto.AiDtos.ChatRequest;
import gov.prajadisha.backend.ai.dto.AiDtos.ChatResponse;
import gov.prajadisha.backend.ai.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return aiChatService.reply(req.text());
    }
}
