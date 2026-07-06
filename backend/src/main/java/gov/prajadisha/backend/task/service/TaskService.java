package gov.prajadisha.backend.task.service;

import gov.prajadisha.backend.ai.service.AiTriageService;
import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Formats;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.org.service.OrganizationService;
import gov.prajadisha.backend.task.dto.TaskDtos.Assignment;
import gov.prajadisha.backend.task.dto.TaskDtos.CommentRequest;
import gov.prajadisha.backend.task.dto.TaskDtos.DashboardStats;
import gov.prajadisha.backend.task.dto.TaskDtos.DashboardTaskRow;
import gov.prajadisha.backend.task.dto.TaskDtos.NoteRequest;
import gov.prajadisha.backend.task.dto.TaskDtos.TaskDetailPayload;
import gov.prajadisha.backend.task.model.DetailedActivity;
import gov.prajadisha.backend.task.model.DetailedComment;
import gov.prajadisha.backend.task.model.DetailedNote;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.event.TicketCreatedEvent;
import gov.prajadisha.backend.task.repository.TaskRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final OrganizationService organizations;
    private final AiTriageService triage;
    private final ApplicationEventPublisher events;

    public TaskService(TaskRepository tasks, OrganizationService organizations,
                       AiTriageService triage, ApplicationEventPublisher events) {
        this.tasks = tasks;
        this.organizations = organizations;
        this.triage = triage;
        this.events = events;
    }

    // ---------------------------------------------------------------- creation

    /** Creates a ticket in "Submitted" state and kicks off async auto-triage. */
    public Task createTicket(String citizenUserName, String title, String description,
                             String locationAddress, String imageUrl) {
        long now = System.currentTimeMillis();
        String id = uniqueTicketId();
        Task task = Task.builder()
                .id(id)
                .groupId(id)
                .parentTaskId(null)
                .orgId(safeActiveOrgId())
                .citizenUserName(citizenUserName)
                .title(title)
                .description(description)
                .voiceUrl("")
                .imageUrl(imageUrl == null ? "" : imageUrl)
                .language("English")
                .location(new Task.TaskLocation(
                        locationAddress == null ? "" : locationAddress,
                        GeoPoint.of(0, 0)))
                .category("Uncategorized")
                .priority("P2")
                .globalStatus("Submitted")
                .isReviewed(false)
                .dueDate(now + 3L * 24 * 60 * 60 * 1000)
                .createdAt(now)
                .reporterType("Citizen")
                .voiceDuration("")
                .subTasks(new ArrayList<>())
                .comments(new ArrayList<>())
                .notes(new ArrayList<>())
                .activities(new ArrayList<>(List.of(DetailedActivity.builder()
                        .timestamp(Formats.dateTime(now))
                        .action("SUBMITTED")
                        .performedBy(citizenUserName)
                        .remarks("Ticket submitted by citizen")
                        .build())))
                .build();
        Task saved = tasks.save(task);
        events.publishEvent(new TicketCreatedEvent(saved.getId()));
        return saved;
    }

    /** Runs classification and moves the ticket to AI-Assigned. Invoked by the async listener. */
    public void autoTriage(String taskId) {
        tasks.findById(taskId).ifPresent(task -> {
            AiTriageService.Classification c = triage.classify(task);
            task.setCategory(c.category());
            task.setPriority(c.priority());
            task.setGlobalStatus("AI-Assigned");
            task.getActivities().add(DetailedActivity.builder()
                    .timestamp(Formats.dateTime(System.currentTimeMillis()))
                    .action("AI_ASSIGNED")
                    .performedBy("system_ai")
                    .remarks("Auto-classified as " + c.category() + " / " + c.priority()
                            + " and routed to " + c.suggestedDepartment())
                    .build());
            tasks.save(task);
        });
    }

    // ------------------------------------------------------------------ lookup

    public Task get(String id) {
        return tasks.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found: " + id));
    }

    // --------------------------------------------------------------- dashboard

    public List<DashboardTaskRow> dashboardTasks(String statusType, String priority,
                                                 int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize > 0 ? pageSize : 50,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Task> list;
        if (priority != null && !priority.isBlank()) {
            list = tasks.findByPriority(priority, pageable);
        } else {
            list = tasks.findByOrgId(safeActiveOrgId(), pageable);
        }

        return list.stream()
                .filter(t -> statusType == null || statusType.isBlank()
                        || statusType.equalsIgnoreCase(statusType(t)))
                .map(this::toRow)
                .toList();
    }

    public DashboardStats stats() {
        List<Task> all = tasks.findByOrgId(safeActiveOrgId(), Pageable.unpaged());
        long awaitingAiReview = all.stream()
                .filter(t -> "AI-Assigned".equalsIgnoreCase(t.getGlobalStatus()) && !t.isReviewed())
                .count();
        long dueToday = all.stream()
                .filter(t -> Formats.isToday(t.getDueDate())
                        && !"Resolved".equalsIgnoreCase(t.getGlobalStatus()))
                .count();
        long myDept = all.size();
        return new DashboardStats(awaitingAiReview, dueToday, myDept);
    }

    private DashboardTaskRow toRow(Task t) {
        List<Assignment> assignments = new ArrayList<>();
        if (t.getCategory() != null && !t.getCategory().isBlank()) {
            assignments.add(Assignment.dept(t.getCategory()));
        }

        String statusType = statusType(t);
        boolean critical = Formats.isToday(t.getDueDate())
                && !"Resolved".equalsIgnoreCase(t.getGlobalStatus());

        return new DashboardTaskRow(
                t.getId(),
                t.getTitle(),
                t.getPriority(),
                assignments,
                Formats.fullDate(t.getDueDate()),
                critical,
                statusLabel(t),
                statusType,
                indicatorColor(statusType));
    }

    private String statusType(Task t) {
        String s = t.getGlobalStatus() == null ? "" : t.getGlobalStatus().toLowerCase();
        if (s.contains("ai-assigned") && !t.isReviewed()) return "ai-pending";
        if (s.contains("progress")) return "in-progress";
        if (s.contains("resolved")) return "resolved";
        if (s.contains("rejected")) return "rejected";
        return "open";
    }

    private String statusLabel(Task t) {
        if ("ai-pending".equals(statusType(t))) return "AI-Assigned: Review Pending";
        return t.getGlobalStatus();
    }

    private String indicatorColor(String statusType) {
        return switch (statusType) {
            case "ai-pending" -> "accent-blue";
            case "in-progress" -> "accent-amber";
            case "resolved" -> "accent-green";
            case "rejected" -> "accent-red";
            default -> "accent-gray";
        };
    }

    // ------------------------------------------------------------------ detail

    public TaskDetailPayload detail(String id) {
        Task t = get(id);
        String lat = "0", lng = "0";
        String address = "";
        if (t.getLocation() != null) {
            address = t.getLocation().getAddress();
            GeoPoint geo = t.getLocation().getGeo();
            if (geo != null && geo.getCoordinates() != null && geo.getCoordinates().size() == 2) {
                lng = String.valueOf(geo.getCoordinates().get(0));
                lat = String.valueOf(geo.getCoordinates().get(1));
            }
        }
        String mapUrl = "https://maps.google.com/?q=" + lat + "," + lng;

        return new TaskDetailPayload(
                t.getId(),
                t.getTitle(),
                t.getPriority(),
                t.getGroupId(),
                t.getParentTaskId(),
                t.getOrgId(),
                Formats.dateTime(t.getCreatedAt()),
                t.getCitizenUserName(),
                t.getReporterType() == null ? "Citizen" : t.getReporterType(),
                t.getDescription(),
                t.getVoiceUrl(),
                t.getVoiceDuration() == null ? "" : t.getVoiceDuration(),
                t.getCategory(),
                t.getLanguage(),
                new TaskDetailPayload.Location(address, lat, lng),
                t.getImageUrl(),
                mapUrl,
                t.getSubTasks(),
                t.getComments(),
                t.getNotes(),
                t.getActivities());
    }

    // ---------------------------------------------------------- comments/notes

    public TaskDetailPayload addComment(String id, CommentRequest req) {
        Task t = get(id);
        String author = req.userName() == null ? "Officer" : req.userName();
        long now = System.currentTimeMillis();
        t.getComments().add(DetailedComment.builder()
                .userName(author)
                .initials(Ids.initials(author))
                .timestamp(Formats.dateTime(now))
                .text(req.text())
                .isSelf(req.isOfficer())
                .build());
        t.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(now))
                .action("COMMENT_ADDED")
                .performedBy(author)
                .remarks("Public comment added")
                .build());
        tasks.save(t);
        return detail(id);
    }

    public TaskDetailPayload addNote(String id, NoteRequest req) {
        Task t = get(id);
        String author = req.userName() == null ? "Officer" : req.userName();
        long now = System.currentTimeMillis();
        t.getNotes().add(DetailedNote.builder()
                .userName(author)
                .timestamp(Formats.dateTime(now))
                .text(req.text())
                .build());
        t.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(now))
                .action("NOTE_ADDED")
                .performedBy(author)
                .remarks("Internal note added")
                .build());
        tasks.save(t);
        return detail(id);
    }

    // ------------------------------------------------------------ status moves

    public Task reopen(String id) {
        Task t = get(id);
        t.setGlobalStatus("In Progress");
        t.setReviewed(false);
        t.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                .action("STATUS_CHANGED")
                .performedBy(t.getCitizenUserName())
                .remarks("Ticket reopened by citizen")
                .build());
        return tasks.save(t);
    }

    public Task recordFeedback(String id, int rating, String comment) {
        Task t = get(id);
        long now = System.currentTimeMillis();
        if (comment != null && !comment.isBlank()) {
            t.getComments().add(DetailedComment.builder()
                    .userName(t.getCitizenUserName())
                    .initials(Ids.initials(t.getCitizenUserName()))
                    .timestamp(Formats.dateTime(now))
                    .text(comment)
                    .isSelf(false)
                    .build());
        }
        t.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(now))
                .action("FEEDBACK_SUBMITTED")
                .performedBy(t.getCitizenUserName())
                .remarks("Citizen rated resolution " + rating + "/5")
                .build());
        return tasks.save(t);
    }

    // ----------------------------------------------------------------- helpers

    private String uniqueTicketId() {
        String id;
        do {
            id = Ids.ticketId();
        } while (tasks.existsById(id));
        return id;
    }

    private String safeActiveOrgId() {
        try {
            return organizations.getActiveOrgId();
        } catch (ApiException e) {
            return "ORG-101";
        }
    }

    public Optional<Task> findById(String id) {
        return tasks.findById(id);
    }
}
