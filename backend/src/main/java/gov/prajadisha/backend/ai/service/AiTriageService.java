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

    public record Classification(String category, String priority, String suggestedDepartment, String title, String language) {}

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
                route it to the single most appropriate department, generate a short, concise,
                actionable title (max 5-7 words) for the ticket based on the description, location, or voice transcript,
                and detect the primary language of the ticket text (e.g. "English", "Kannada", "Hindi", "Telugu", "Tamil", etc.).
                If the text is in an Indian language but written using English script (transliterated), identify it as the respective Indian language (e.g. "Kannada" or "Hindi").
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
                Also generate a short summary title.
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
        String title = result.path("title").asText("").trim();
        if (title.isBlank()) {
            title = generateFallbackTitle(task.getDescription());
        }
        String language = result.path("language").asText("").trim();
        if (language.isBlank()) {
            language = "English";
        }

        return new Classification(category, priority, department, title, language);
    }

    private Map<String, Object> classificationSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("category", Map.of("type", "string"));
        props.put("priority", Map.of("type", "string"));
        props.put("suggestedDepartment", Map.of("type", "string"));
        props.put("title", Map.of("type", "string"));
        props.put("language", Map.of("type", "string"));
        schema.put("properties", props);
        schema.put("required", List.of("category", "priority", "suggestedDepartment", "title", "language"));
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

    private Classification classifyByKeyword(Task task) {
        String text = (nullToEmpty(task.getTitle()) + " " + nullToEmpty(task.getDescription()))
                .toLowerCase(Locale.ROOT);

        // 1. Resolve Category dynamically based on OrganizationConfig categories
        String category = "Grievance";
        List<OrganizationConfig.OrgCategory> categories = categoriesFor(task.getOrgId());
        if (!categories.isEmpty()) {
            category = categories.get(0).getName(); // default to first category
            for (OrganizationConfig.OrgCategory cat : categories) {
                String catName = cat.getName().toLowerCase();
                String catDesc = nullToEmpty(cat.getDescription()).toLowerCase();
                if (text.contains(catName) || (!catDesc.isEmpty() && containsAnyWord(text, catDesc))) {
                    category = cat.getName();
                    break;
                }
            }
        }

        // 2. Resolve Department dynamically based on Organization's departments
        String department = "General Administration";
        List<Department> orgDepts = departments.findByOrgId(task.getOrgId());
        if (!orgDepts.isEmpty()) {
            Department matchedDept = null;
            for (Department d : orgDepts) {
                String deptName = d.getName().toLowerCase();
                String deptRole = nullToEmpty(d.getRoleDescription()).toLowerCase();
                if (text.contains(deptName) || (!deptRole.isEmpty() && containsAnyWord(text, deptRole))) {
                    matchedDept = d;
                    break;
                }
            }
            if (matchedDept != null) {
                department = matchedDept.getName();
            } else {
                department = orgDepts.get(0).getName();
            }
        }

        // 3. Generate fallback title dynamically
        String title = generateFallbackTitle(task.getDescription());

        // 4. Resolve Priority dynamically based on keywords
        String priority = "P2";
        List<String> priorities = prioritiesFor(task.getOrgId());
        if (!priorities.isEmpty()) {
            priority = priorities.size() > 2 ? priorities.get(2) : priorities.get(0);
        }
        
        if (containsAny(text, "danger", "urgent", "accident", "fire", "collapse", "electric", "shock", "safety")) {
            priority = priorities.isEmpty() ? "P0" : priorities.get(0);
        } else if (containsAny(text, "broken", "not working", "flicker", "overflow", "block", "leak")) {
            priority = priorities.isEmpty() ? "P1" : (priorities.size() > 1 ? priorities.get(1) : priorities.get(0));
        }

        return new Classification(category, priority, department, title, "English");
    }

    private boolean containsAnyWord(String text, String sentence) {
        String[] words = sentence.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        for (String w : words) {
            if (w.length() > 3 && text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private String generateFallbackTitle(String description) {
        if (description == null || description.isBlank()) {
            return "Civic Concern";
        }
        String clean = description.trim();
        int sentenceEnd = clean.indexOf('.');
        if (sentenceEnd > 10 && sentenceEnd < 50) {
            return clean.substring(0, sentenceEnd);
        }
        String[] words = clean.split("\\s+");
        if (words.length <= 5) {
            return clean;
        } else {
            return String.join(" ", java.util.Arrays.copyOfRange(words, 0, 5)) + "...";
        }
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
