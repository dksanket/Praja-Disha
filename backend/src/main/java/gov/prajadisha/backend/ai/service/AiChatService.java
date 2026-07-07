package gov.prajadisha.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.prajadisha.backend.ai.dto.AiDtos.ChatResponse;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.service.OrganizationService;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Command Copilot for the Org-Admin dashboard.
 *
 * <p>Assembles a compact snapshot of the active organization's live civic data (ticket counts by
 * status/priority/category, departments, overdue items) and asks an LLM via {@link OllamaClient}
 * for an analytical answer plus follow-up suggestions. Falls back to a deterministic data summary
 * if the model is disabled or unreachable, so the dashboard chat always responds.
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final OllamaClient ollama;
    private final TaskRepository tasks;
    private final DepartmentRepository departments;
    private final OrganizationService organizations;

    public AiChatService(OllamaClient ollama, TaskRepository tasks,
                         DepartmentRepository departments, OrganizationService organizations) {
        this.ollama = ollama;
        this.tasks = tasks;
        this.departments = departments;
        this.organizations = organizations;
    }

    public ChatResponse reply(String userText) {
        long now = System.currentTimeMillis();
        String dataContext = buildDataContext();

        if (ollama.isEnabled()) {
            try {
                return llmReply(userText, dataContext, now);
            } catch (Exception e) {
                log.warn("AI copilot via Ollama failed ({}); using data-summary fallback.", e.getMessage());
            }
        }
        return fallbackReply(userText, dataContext, now);
    }

    // ------------------------------------------------------------------ LLM path

    private ChatResponse llmReply(String userText, String dataContext, long now) {
        String system = """
                You are the AI Command Copilot for a municipal Org-Admin dashboard.
                Answer the officer's question using ONLY the civic data snapshot provided.
                Be concise, specific, and cite concrete numbers from the data.
                If the data does not contain the answer, say so plainly rather than inventing figures.
                Respond ONLY with JSON: {"text": string, "suggestions": string[]} where suggestions
                are up to 3 short, relevant follow-up questions the officer might ask next.
                """;
        String user = "Civic data snapshot:\n" + dataContext + "\n\nOfficer question: " + userText;

        JsonNode result = ollama.chatJson(system, user, chatSchema());
        String text = result.path("text").asText("");
        if (text.isBlank()) {
            text = ollama.chat(system, user);
        }

        List<String> suggestions = new ArrayList<>();
        JsonNode s = result.path("suggestions");
        if (s.isArray()) {
            s.forEach(n -> {
                String v = n.asText("");
                if (!v.isBlank()) suggestions.add(v);
            });
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(defaultSuggestions());
        }
        return new ChatResponse("msg-ai-" + now, "ai", text, now, suggestions);
    }

    private Map<String, Object> chatSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("text", Map.of("type", "string"));
        props.put("suggestions", Map.of("type", "array", "items", Map.of("type", "string")));
        schema.put("properties", props);
        schema.put("required", List.of("text", "suggestions"));
        return schema;
    }

    // -------------------------------------------------------------- fallback path

    private ChatResponse fallbackReply(String userText, String dataContext, long now) {
        String text = "Here is a live summary of the civic data for your query \"" + userText + "\":\n"
                + dataContext
                + "\n(Set an Ollama Cloud API key in application.properties to enable full "
                + "natural-language analysis.)";
        return new ChatResponse("msg-ai-" + now, "ai", text, now, defaultSuggestions());
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "Which departments have the most overdue tickets this week?",
                "Summarize the highest-priority open issues.",
                "Draft a public update for the most urgent unresolved ticket.");
    }

    // ---------------------------------------------------------- data collection

    private String buildDataContext() {
        String orgId;
        String orgName;
        try {
            var org = organizations.getActive();
            orgId = org.getId();
            orgName = org.getName();
        } catch (Exception e) {
            return "No organization data available.";
        }

        List<Task> all = tasks.findByOrgId(orgId, Pageable.unpaged());
        long total = all.size();

        Map<String, Long> byStatus = all.stream().collect(Collectors.groupingBy(
                t -> t.getGlobalStatus() == null ? "Unknown" : t.getGlobalStatus(), Collectors.counting()));
        Map<String, Long> byPriority = all.stream().collect(Collectors.groupingBy(
                t -> t.getPriority() == null ? "Unknown" : t.getPriority(), Collectors.counting()));
        Map<String, Long> byCategory = all.stream().collect(Collectors.groupingBy(
                t -> t.getCategory() == null ? "Uncategorized" : t.getCategory(), Collectors.counting()));

        long nowMs = System.currentTimeMillis();
        long overdue = all.stream()
                .filter(t -> t.getDueDate() > 0 && t.getDueDate() < nowMs
                        && !"Resolved".equalsIgnoreCase(t.getGlobalStatus()))
                .count();

        List<Department> depts = departments.findByOrgId(orgId);

        StringBuilder sb = new StringBuilder();
        sb.append("Organization: ").append(orgName).append(" (").append(orgId).append(")\n");
        sb.append("Total tickets: ").append(total).append("\n");
        sb.append("Overdue & unresolved: ").append(overdue).append("\n");
        sb.append("By status: ").append(mapToString(byStatus)).append("\n");
        sb.append("By priority: ").append(mapToString(byPriority)).append("\n");
        sb.append("By category: ").append(mapToString(byCategory)).append("\n");
        sb.append("Departments (").append(depts.size()).append("): ")
                .append(depts.stream().map(Department::getName).collect(Collectors.joining(", ")));
        return sb.toString();
    }

    private String mapToString(Map<String, Long> map) {
        if (map.isEmpty()) return "none";
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
