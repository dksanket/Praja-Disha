package gov.prajadisha.backend.ai.service;

import gov.prajadisha.backend.task.model.Task;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Auto-triage for incoming tickets.
 *
 * <p>This is a deterministic, keyword-based stand-in for an LLM classifier. Swap the body of
 * {@link #classify(Task)} for a call to your model provider (e.g. the Anthropic Messages API)
 * to enable real category/priority routing. The method is invoked asynchronously after a
 * citizen submits a ticket.
 */
@Service
public class AiTriageService {

    public record Classification(String category, String priority, String suggestedDepartment) {}

    public Classification classify(Task task) {
        String text = ((task.getTitle() == null ? "" : task.getTitle()) + " "
                + (task.getDescription() == null ? "" : task.getDescription()))
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

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}
