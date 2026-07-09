package gov.prajadisha.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.prajadisha.backend.ai.dto.AiDtos.ChatResponse;
import gov.prajadisha.backend.ai.model.AiChatMessage;
import gov.prajadisha.backend.ai.repository.AiChatMessageRepository;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import gov.prajadisha.backend.org.service.OrganizationService;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.model.TaskAssignment;
import gov.prajadisha.backend.task.repository.TaskAssignmentRepository;
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
    private final AiChatMessageRepository aiChatMessages;
    private final TaskAssignmentRepository taskAssignments;
    private final OfficerRepository officers;

    public AiChatService(OllamaClient ollama, TaskRepository tasks,
                         DepartmentRepository departments, OrganizationService organizations,
                         AiChatMessageRepository aiChatMessages, TaskAssignmentRepository taskAssignments,
                         OfficerRepository officers) {
        this.ollama = ollama;
        this.tasks = tasks;
        this.departments = departments;
        this.organizations = organizations;
        this.aiChatMessages = aiChatMessages;
        this.taskAssignments = taskAssignments;
        this.officers = officers;
    }

    public ChatResponse reply(String userText) {
        return reply(userText, "aarav_sharma");
    }

    public ChatResponse reply(String userText, String officerUserName) {
        long now = System.currentTimeMillis();
        String orgId;
        try {
            orgId = organizations.getActiveOrgId();
        } catch (Exception e) {
            orgId = organizations.findAll().stream()
                    .findFirst()
                    .map(gov.prajadisha.backend.org.model.Organization::getId)
                    .orElse("unknown-org");
        }

        // 1. Save user message to database
        long userTimestamp = now;
        AiChatMessage userMsg = AiChatMessage.builder()
                .id("msg-user-" + userTimestamp)
                .officerUserName(officerUserName)
                .orgId(orgId)
                .sender("user")
                .text(userText)
                .timestamp(userTimestamp)
                .build();
        aiChatMessages.save(userMsg);

        // 2. Build context snapshot
        String dataContext = buildDataContext(orgId);

        // 3. Generate response (either via LLM or fallback)
        if (ollama.isEnabled()) {
            try {
                return llmReply(userText, dataContext, officerUserName, orgId, userTimestamp);
            } catch (Exception e) {
                log.warn("AI copilot via Ollama failed ({}); using data-summary fallback.", e.getMessage());
            }
        }
        return fallbackReply(userText, dataContext, officerUserName, orgId, userTimestamp);
    }

    public List<ChatResponse> getHistory(String officerUserName) {
        String orgId;
        try {
            orgId = organizations.getActiveOrgId();
        } catch (Exception e) {
            orgId = organizations.findAll().stream()
                    .findFirst()
                    .map(gov.prajadisha.backend.org.model.Organization::getId)
                    .orElse("unknown-org");
        }
        List<AiChatMessage> messages = aiChatMessages.findByOfficerUserNameAndOrgIdOrderByTimestampAsc(officerUserName, orgId);
        return messages.stream()
                .map(m -> new ChatResponse(m.getId(), m.getSender(), m.getText(), m.getTimestamp(), m.getSuggestions()))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ LLM path

    private ChatResponse llmReply(String userText, String dataContext, String officerUserName, String orgId, long userTimestamp) {
        String system = """
                You are the AI Command Copilot for a municipal Org-Admin dashboard.
                Answer the officer's question using ONLY the civic data snapshot provided.
                Be concise, specific, and cite concrete numbers from the data.
                If the data does not contain the answer, say so plainly rather than inventing figures.
                Respond ONLY with JSON: {"text": string, "suggestions": string[]} where suggestions
                are up to 3 short, relevant follow-up questions the officer might ask next.

                Civic data snapshot:
                """ + dataContext;

        // Fetch last 10 messages from DB before the new userMsg (which has timestamp = userTimestamp)
        List<AiChatMessage> dbHistory = aiChatMessages.findByOfficerUserNameAndOrgIdOrderByTimestampAsc(officerUserName, orgId);
        List<Map<String, String>> ollamaHistory = new ArrayList<>();
        List<AiChatMessage> pastMessages = dbHistory.stream()
                .filter(m -> m.getTimestamp() < userTimestamp)
                .collect(Collectors.toList());
        int startIdx = Math.max(0, pastMessages.size() - 10);
        for (int i = startIdx; i < pastMessages.size(); i++) {
            AiChatMessage msg = pastMessages.get(i);
            ollamaHistory.add(Map.of(
                "role", "user".equals(msg.getSender()) ? "user" : "assistant",
                "content", msg.getText()
            ));
        }

        JsonNode result = ollama.chatJson(system, ollamaHistory, userText, chatSchema());
        String text = result.path("text").asText("");
        if (text.isBlank()) {
            text = ollama.chat(system, ollamaHistory, userText);
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

        long aiTime = System.currentTimeMillis();
        AiChatMessage aiMsg = AiChatMessage.builder()
                .id("msg-ai-" + aiTime)
                .officerUserName(officerUserName)
                .orgId(orgId)
                .sender("ai")
                .text(text)
                .timestamp(aiTime)
                .suggestions(suggestions)
                .build();
        aiChatMessages.save(aiMsg);

        return new ChatResponse("msg-ai-" + aiTime, "ai", text, aiTime, suggestions);
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

    private ChatResponse fallbackReply(String userText, String dataContext, String officerUserName, String orgId, long userTimestamp) {
        String text = "Here is a live summary of the civic data for your query \"" + userText + "\":\n"
                + dataContext
                + "\n(Set an Ollama Cloud API key in application.properties to enable full "
                + "natural-language analysis.)";
        List<String> suggestions = defaultSuggestions();

        long aiTime = System.currentTimeMillis();
        AiChatMessage aiMsg = AiChatMessage.builder()
                .id("msg-ai-" + aiTime)
                .officerUserName(officerUserName)
                .orgId(orgId)
                .sender("ai")
                .text(text)
                .timestamp(aiTime)
                .suggestions(suggestions)
                .build();
        aiChatMessages.save(aiMsg);

        return new ChatResponse("msg-ai-" + aiTime, "ai", text, aiTime, suggestions);
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "Which departments have the most overdue tickets this week?",
                "Summarize the highest-priority open issues.",
                "Draft a public update for the most urgent unresolved ticket.");
    }

    // ---------------------------------------------------------- data collection

    private String buildDataContext(String orgId) {
        String orgName = "BBMP";
        try {
            var org = organizations.getActive();
            orgName = org.getName();
        } catch (Exception e) {
            // ignore
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
        Map<String, String> deptNameMap = depts.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        String currentDateStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(nowMs));
        sb.append("Current Date: ").append(currentDateStr).append("\n");
        sb.append("Organization: ").append(orgName).append(" (").append(orgId).append(")\n");
        sb.append("Total tickets: ").append(total).append("\n");
        sb.append("Overdue & unresolved: ").append(overdue).append("\n");
        sb.append("By status: ").append(mapToString(byStatus)).append("\n");
        sb.append("By priority: ").append(mapToString(byPriority)).append("\n");
        sb.append("By category: ").append(mapToString(byCategory)).append("\n");
        sb.append("Departments (").append(depts.size()).append("): ")
                .append(depts.stream().map(Department::getName).collect(Collectors.joining(", "))).append("\n");

        sb.append("\nDetailed Task List:\n");
        for (Task t : all) {
            String createdStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(t.getCreatedAt()));

            // Format due date (display "None" if not set)
            String dueStr = t.getDueDate() > 0 ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(t.getDueDate())) : "None";

            // Find resolved assigned departments
            List<TaskAssignment> assignments = taskAssignments.findByTaskId(t.getId());
            String assignedDepts = assignments.stream()
                    .map(a -> deptNameMap.getOrDefault(a.getDepartmentId(), a.getDepartmentId()))
                    .collect(Collectors.joining(", "));
            if (assignedDepts.isEmpty()) {
                assignedDepts = t.getCategory() != null ? t.getCategory() : "None";
            }

            sb.append(String.format("- Task ID: %s | Title: %s | Created: %s | Due Date: %s | Status: %s | Priority: %s | Category: %s | Department: %s | Description: %s\n",
                    t.getId(),
                    t.getTitle(),
                    createdStr,
                    dueStr,
                    t.getGlobalStatus(),
                    t.getPriority(),
                    t.getCategory(),
                    assignedDepts,
                    t.getDescription() == null ? "" : t.getDescription()));
        }

        return sb.toString();
    }

    private String mapToString(Map<String, Long> map) {
        if (map.isEmpty()) return "none";
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
