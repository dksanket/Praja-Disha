package gov.prajadisha.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.OrganizationConfig;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OrganizationConfigRepository;
import gov.prajadisha.backend.task.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Auto-triage for incoming tickets.
 *
 * <p>Primary path: sends the ticket text plus the organization's configured categories, priorities,
 * departments and custom routing rules to an LLM via {@link OllamaClient}, constrained to a JSON
 * schema so the response maps cleanly onto {@link Classification}. If the model is disabled,
 * unreachable, or returns something unusable, it transparently falls back to the deterministic
 * keyword classifier so triage always produces a result.
 */
@Service
public class AiTriageService {

    private static final Logger log = LoggerFactory.getLogger(AiTriageService.class);

    private final OllamaClient ollama;
    private final OrganizationConfigRepository orgConfigs;
    private final DepartmentRepository departments;

    public AiTriageService(OllamaClient ollama,
                           OrganizationConfigRepository orgConfigs,
                           DepartmentRepository departments) {
        this.ollama = ollama;
        this.orgConfigs = orgConfigs;
        this.departments = departments;
    }

    public record Classification(String category, String priority, String suggestedDepartment) {}

    public Classification classify(Task task) {
        if (ollama.isEnabled()) {
            try {
                Classification c = classifyWithLlm(task);
                if (c != null) {
                    return c;
                }
            } catch (Exception e) {
                log.warn("AI triage via Ollama failed ({}); using keyword fallback.", e.getMessage());
            }
        }
        return classifyByKeyword(task);
    }

    // ------------------------------------------------------------------ LLM path

    private Classification classifyWithLlm(Task task) {
        List<OrganizationConfig.OrgCategory> categories = categoriesFor(task.getOrgId());
        List<String> priorities = prioritiesFor(task.getOrgId());
        List<Department> depts = departments.findByOrgId(task.getOrgId());

        List<String> categoryNames = categories.stream()
                .map(OrganizationConfig.OrgCategory::getName).toList();
        List<String> deptNames = depts.stream().map(Department::getName).toList();

        String catBlock = categories.isEmpty()
                ? "Infrastructure, Sanitation, Water Supply, Horticulture, Grievance"
                : categories.stream()
                    .map(c -> "- " + c.getName() + ": " + nullToEmpty(c.getDescription()))
                    .collect(Collectors.joining("\n"));

        String deptBlock = depts.isEmpty()
                ? "- General Administration: catch-all department"
                : depts.stream()
                    .map(d -> "- " + d.getName() + ": " + nullToEmpty(d.getRoleDescription()))
                    .collect(Collectors.joining("\n"));

        String priorityList = priorities.isEmpty()
                ? "P0 (critical/public-safety), P1 (high), P2 (normal), P3 (low)"
                : String.join(", ", priorities);

        String customRules = orgConfigs.findByOrgId(task.getOrgId())
                .map(OrganizationConfig::getCustomPromptExtension)
                .filter(s -> s != null && !s.isBlank())
                .orElse("");

        String system = """
                You are the auto-triage router for a municipal civic-issue platform.
                Classify each citizen-reported ticket into exactly one category and one priority,
                and route it to the single most appropriate department.
                Treat public-safety hazards (electrical, structural collapse, fire, flooding, accidents)
                as the highest priority. Respond ONLY with JSON matching the requested schema.
                """
                + (customRules.isEmpty() ? "" : "\nOrganization-specific rules: " + customRules);

        String user = """
                Ticket title: %s
                Ticket description: %s
                Reported location: %s

                Available categories:
                %s

                Available priorities (most to least urgent): %s

                Available departments:
                %s

                Pick the best-fitting category name, priority, and department name from the lists above.
                """.formatted(
                        nullToEmpty(task.getTitle()),
                        nullToEmpty(task.getDescription()),
                        task.getLocation() == null ? "" : nullToEmpty(task.getLocation().getAddress()),
                        catBlock, priorityList, deptBlock);

        JsonNode result = ollama.chatJson(system, user, classificationSchema());
        String category = firstNonBlank(result.path("category").asText(""),
                categoryNames.isEmpty() ? "Grievance" : categoryNames.get(0));
        String priority = firstNonBlank(result.path("priority").asText(""),
                priorities.isEmpty() ? "P2" : priorities.get(priorities.size() > 2 ? 2 : 0));
        String department = firstNonBlank(result.path("suggestedDepartment").asText(""),
                deptNames.isEmpty() ? "General Administration" : deptNames.get(0));

        return new Classification(category, priority, department);
    }

    private Map<String, Object> classificationSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("category", Map.of("type", "string"));
        props.put("priority", Map.of("type", "string"));
        props.put("suggestedDepartment", Map.of("type", "string"));
        schema.put("properties", props);
        schema.put("required", List.of("category", "priority", "suggestedDepartment"));
        return schema;
    }

    private List<OrganizationConfig.OrgCategory> categoriesFor(String orgId) {
        return orgConfigs.findByOrgId(orgId)
                .map(OrganizationConfig::getCategories)
                .orElseGet(ArrayList::new);
    }

    private List<String> prioritiesFor(String orgId) {
        return orgConfigs.findByOrgId(orgId)
                .map(OrganizationConfig::getPriorities)
                .filter(p -> p != null && !p.isEmpty())
                .orElseGet(() -> List.of("P0", "P1", "P2", "P3"));
    }

    // -------------------------------------------------------------- keyword path

    private Classification classifyByKeyword(Task task) {
        String text = (nullToEmpty(task.getTitle()) + " " + nullToEmpty(task.getDescription()))
                .toLowerCase(Locale.ROOT);

        String category = "Grievance";
        String department = "General Administration";
        if (containsAny(text, "streetlight", "light", "road", "pothole", "signal", "footpath", "bridge")) {
            category = "Infrastructure";
            department = "Roads & Streetlights";
        } else if (containsAny(text, "garbage", "waste", "sewage", "drain", "sanitation", "toilet")) {
            category = "Sanitation";
            department = "Sanitation";
        } else if (containsAny(text, "water", "supply", "leak", "pipe")) {
            category = "Water Supply";
            department = "Water Board";
        } else if (containsAny(text, "tree", "park", "garden", "horticulture")) {
            category = "Horticulture";
            department = "Horticulture";
        }

        String priority = "P2";
        if (containsAny(text, "danger", "urgent", "accident", "fire", "collapse", "electric", "shock")) {
            priority = "P0";
        } else if (containsAny(text, "broken", "not working", "flicker", "overflow", "block")) {
            priority = "P1";
        }

        return new Classification(category, priority, department);
    }

    // ----------------------------------------------------------------- helpers

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
