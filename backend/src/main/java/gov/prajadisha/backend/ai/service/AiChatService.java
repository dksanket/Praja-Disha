package gov.prajadisha.backend.ai.service;

import gov.prajadisha.backend.ai.dto.AiDtos.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Command Copilot.
 *
 * <p>Returns a structured, deterministic response so the dashboard chat is fully wired end-to-end.
 * To make it "real", replace {@link #reply(String)} with a call to your LLM provider (e.g. the
 * Anthropic Messages API using {@code claude-opus-4-8}) and map the model output into
 * {@link ChatResponse}, keeping the {@code suggestions} follow-up prompts.
 */
@Service
public class AiChatService {

    public ChatResponse reply(String userText) {
        long now = System.currentTimeMillis();
        String text = "Here is a summary based on the available civic data for your query: \""
                + userText + "\". "
                + "Connect an LLM provider in AiChatService#reply to generate live analytical responses.";
        List<String> suggestions = List.of(
                "Compare resident satisfaction rates between District A and District B.",
                "Which departments have the most overdue tickets this week?",
                "Draft a public update for the highest-priority open issue.");
        return new ChatResponse("msg-ai-" + now, "ai", text, now, suggestions);
    }
}
