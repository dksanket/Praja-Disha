package gov.prajadisha.backend.task.service;

import gov.prajadisha.backend.ai.service.AiTriageService;
import gov.prajadisha.backend.ai.service.GoogleSpeechService;
import gov.prajadisha.backend.ai.service.OllamaClient;
import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Formats;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.org.service.OrganizationService;
import gov.prajadisha.backend.storage.StorageService;
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
import gov.prajadisha.backend.task.model.TaskAssignment;
import gov.prajadisha.backend.task.repository.TaskAssignmentRepository;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OfficerRepository;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository tasks;
    private final OrganizationService organizations;
    private final AiTriageService triage;
    private final OllamaClient ollama;
    private final ApplicationEventPublisher events;
    private final StorageService storageService;
    private final GoogleSpeechService speechService;
    private final DepartmentRepository departments;
    private final OfficerRepository officers;
    private final TaskAssignmentRepository taskAssignments;

    public TaskService(TaskRepository tasks, OrganizationService organizations,
                       AiTriageService triage, OllamaClient ollama,
                       ApplicationEventPublisher events, StorageService storageService,
                       GoogleSpeechService speechService, DepartmentRepository departments,
                       OfficerRepository officers, TaskAssignmentRepository taskAssignments) {
        this.tasks = tasks;
        this.organizations = organizations;
        this.triage = triage;
        this.ollama = ollama;
        this.events = events;
        this.storageService = storageService;
        this.speechService = speechService;
        this.departments = departments;
        this.officers = officers;
        this.taskAssignments = taskAssignments;
    }


    // ---------------------------------------------------------------- creation

    public Task createTicket(String citizenUserName, String title, String description,
                             String locationAddress, Double latitude, Double longitude, String imageUrl, String voiceUrl, String voiceDuration,
                             List<String> mediaUrls, String language) {
        long now = System.currentTimeMillis();
        String id = uniqueTicketId();

        List<String> urls = mediaUrls == null ? new ArrayList<>() : mediaUrls;
        String resolvedImageUrl = imageUrl;
        if ((resolvedImageUrl == null || resolvedImageUrl.isBlank()) && !urls.isEmpty()) {
            resolvedImageUrl = urls.get(0);
        }
        if (resolvedImageUrl == null) {
            resolvedImageUrl = "";
        }

        Task task = Task.builder()
                .id(id)
                .groupId(id)
                .parentTaskId(null)
                .orgId(safeActiveOrgId())
                .citizenUserName(citizenUserName)
                .title(title == null || title.isBlank() ? "Processing AI Title..." : title)
                .description(description)
                .voiceUrl(voiceUrl == null ? "" : voiceUrl)
                .imageUrl(resolvedImageUrl)
                .mediaUrls(urls)
                .language(language != null && !language.isBlank() ? language : "English")
                .location(new Task.TaskLocation(
                        locationAddress == null ? "" : locationAddress,
                        GeoPoint.of(longitude != null ? longitude : 0.0, latitude != null ? latitude : 0.0)))
                .category("Uncategorized")
                .priority("P2")
                .globalStatus("Submitted")
                .isReviewed(false)
                .dueDate(now + 3L * 24 * 60 * 60 * 1000)
                .createdAt(now)
                .reporterType("Citizen")
                .voiceDuration(voiceDuration == null ? "" : voiceDuration)
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
            // Process voice message if present
            if (task.getVoiceUrl() != null && !task.getVoiceUrl().isBlank()) {
                log.info("Processing voice note for ticket {}: {}", task.getId(), task.getVoiceUrl());
                try {
                    byte[] audioBytes = storageService.loadFileBytes(task.getVoiceUrl());
                    if (audioBytes != null && audioBytes.length > 0) {
                        String speechCode = getSpeechLanguageCode(task.getLanguage());
                        log.info("Transcribing audio using language code: {}", speechCode);
                        String transcript = speechService.transcribe(audioBytes, speechCode);
                        if (transcript != null && !transcript.isBlank()) {
                            log.info("Transcription result: {}", transcript);
                            String englishText = transcript;
                            // Translate to English via Ollama if enabled and language is not English
                            if (!"en-IN".equalsIgnoreCase(speechCode) && ollama.isEnabled()) {
                                log.info("Translating transcript to English via Ollama...");
                                try {
                                    String systemPrompt = "You are a precise translator. Translate the given Indian language text into clear, grammatical English. Do not add any conversational filler, explanations, or notes. Respond only with the English translation.";
                                    String translation = ollama.chat(systemPrompt, transcript);
                                    if (translation != null && !translation.isBlank()) {
                                        englishText = translation.trim();
                                        log.info("Translated text: {}", englishText);
                                    }
                                } catch (OllamaClient.OllamaException e) {
                                    // Translation is best-effort; keep the raw transcript on failure
                                    log.warn("Ollama translation unavailable ({}); using raw transcript as description.", e.getMessage());
                                }
                            } else if (!"en-IN".equalsIgnoreCase(speechCode)) {
                                log.info("Ollama not enabled; storing raw {} transcript as description.", speechCode);
                            }
                            task.setDescription(englishText);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process and transcribe voice note for ticket {}: {}", task.getId(), e.getMessage(), e);
                }
            }

            AiTriageService.Classification c = triage.classify(task);
            task.setCategory(c.category());
            task.setPriority(c.priority());
            task.setGlobalStatus("AI-Assigned");
            if (task.getTitle() == null || task.getTitle().isBlank() || "Processing AI Title...".equals(task.getTitle())) {
                task.setTitle(c.title());
            }
            if (c.language() != null && !c.language().isBlank() && !"English".equalsIgnoreCase(c.language())) {
                task.setLanguage(c.language());
            }

            // Best-effort vector embedding of the ticket text for duplicate detection.
            List<Double> embedding = ollama.embed(
                    (task.getTitle() == null ? "" : task.getTitle()) + ". "
                            + (task.getDescription() == null ? "" : task.getDescription()));
            if (!embedding.isEmpty()) {
                task.setDescriptionEmbedding(embedding);
            }

            // Run duplicate check and update groupId/activities if duplicates exist.
            try {
                checkForDuplicates(task);
            } catch (Exception e) {
                log.error("Error during duplicate checking for task: {}", task.getId(), e);
            }

            task.getActivities().add(DetailedActivity.builder()
                    .timestamp(Formats.dateTime(System.currentTimeMillis()))
                    .action("AI_ASSIGNED")
                    .performedBy("system_ai")
                    .remarks("Auto-classified as " + c.category() + " / " + c.priority()
                            + " and routed to " + c.suggestedDepartment())
                    .build());

            // Create a TaskAssignment in task_assignments collection
            try {
                String deptName = c.suggestedDepartment();
                List<Department> allDepts = departments.findByOrgId(task.getOrgId());
                Department targetDept = allDepts.stream()
                        .filter(d -> deptName.equalsIgnoreCase(d.getName()))
                        .findFirst()
                        .orElseGet(() -> {
                            String text = ((task.getTitle() == null ? "" : task.getTitle()) + " " +
                                           (task.getDescription() == null ? "" : task.getDescription())).toLowerCase();
                            if (text.contains("streetlight") || text.contains("light") || text.contains("electrical")) {
                                return allDepts.stream().filter(d -> "Streetlights & Grid".equalsIgnoreCase(d.getName())).findFirst().orElse(null);
                            }
                            if (text.contains("road") || text.contains("pothole")) {
                                return allDepts.stream().filter(d -> "Road Maintenance".equalsIgnoreCase(d.getName())).findFirst().orElse(null);
                            }
                            return allDepts.isEmpty() ? null : allDepts.get(0);
                        });

                if (targetDept != null) {
                    taskAssignments.deleteByTaskId(task.getId());
                    TaskAssignment assignment = TaskAssignment.builder()
                            .taskId(task.getId())
                            .departmentId(targetDept.getId())
                            .officerId(targetDept.getHeadOfficerId())
                            .status("PENDING")
                            .assignedAt(System.currentTimeMillis())
                            .build();
                    taskAssignments.save(assignment);
                }
            } catch (Exception e) {
                log.error("Failed to auto-assign task {} to department", task.getId(), e);
            }

            tasks.save(task);
        });
    }

    private String getSpeechLanguageCode(String languageName) {
        if (languageName == null) return "en-IN";
        return switch (languageName.trim().toLowerCase()) {
            case "kannada" -> "kn-IN";
            case "hindi" -> "hi-IN";
            case "tamil" -> "ta-IN";
            case "telugu" -> "te-IN";
            case "bengali" -> "bn-IN";
            case "marathi" -> "mr-IN";
            case "gujarati" -> "gu-IN";
            case "malayalam" -> "ml-IN";
            case "punjabi" -> "pa-IN";
            case "urdu" -> "ur-PK";
            case "assamese" -> "as-IN";
            case "odia" -> "or-IN";
            default -> "en-IN";
        };
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
            list = tasks.findByPriorityAndParentTaskIdIsNull(priority, pageable);
        } else {
            list = tasks.findByOrgIdAndParentTaskIdIsNull(safeActiveOrgId(), pageable);
        }

        return list.stream()
                .filter(t -> statusType == null || statusType.isBlank()
                        || statusType.equalsIgnoreCase(statusType(t)))
                .map(this::toRow)
                .toList();
    }

    public DashboardStats stats() {
        List<Task> all = tasks.findByOrgIdAndParentTaskIdIsNull(safeActiveOrgId(), Pageable.unpaged());
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
                indicatorColor(statusType),
                t.getGroupId());
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

        List<Task> subTasks = tasks.findByParentTaskId(t.getId());
        if (subTasks == null) {
            subTasks = new ArrayList<>();
        }

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
                subTasks,
                t.getComments(),
                t.getNotes(),
                t.getActivities(),
                t.getMediaUrls() == null ? new ArrayList<>() : t.getMediaUrls());
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

    /** Returns all tasks submitted by the given citizen, sorted newest first. */
    public List<Task> getTicketsForCitizen(String citizenUserName) {
        return tasks.findByCitizenUserName(citizenUserName).stream()
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .toList();
    }

    private Department findAssignedDepartment(Task t, List<Department> depts) {
        String text = ((t.getTitle() == null ? "" : t.getTitle()) + " " +
                       (t.getDescription() == null ? "" : t.getDescription()) + " " +
                       (t.getCategory() == null ? "" : t.getCategory())).toLowerCase();
        
        // Match specific department names
        for (Department d : depts) {
            if (d.getName() != null && text.contains(d.getName().toLowerCase())) {
                return d;
            }
        }
        // Fallback matching by keyword
        if (text.contains("streetlight") || text.contains("light") || text.contains("electrical")) {
            return depts.stream().filter(d -> "Streetlights & Grid".equalsIgnoreCase(d.getName())).findFirst().orElse(null);
        }
        if (text.contains("road") || text.contains("pothole")) {
            return depts.stream().filter(d -> "Road Maintenance".equalsIgnoreCase(d.getName())).findFirst().orElse(null);
        }
        // General fallback
        return depts.isEmpty() ? null : depts.get(0);
    }

    public Task createSubTask(String parentId, gov.prajadisha.backend.task.dto.TaskDtos.CreateSubTaskRequest req) {
        Task parent = get(parentId);
        long now = System.currentTimeMillis();
        String id = uniqueTicketId();

        Task subTask = Task.builder()
                .id(id)
                .groupId(parent.getGroupId())
                .parentTaskId(parentId)
                .orgId(parent.getOrgId())
                .citizenUserName(parent.getCitizenUserName())
                .title(req.title())
                .description(req.description() != null ? req.description() : "")
                .voiceUrl("")
                .imageUrl("")
                .mediaUrls(new ArrayList<>())
                .language(parent.getLanguage())
                .location(parent.getLocation())
                .category(req.category() != null && !req.category().isBlank() ? req.category() : parent.getCategory())
                .priority(req.priority() != null && !req.priority().isBlank() ? req.priority() : "P2")
                .globalStatus("In Progress")
                .isReviewed(true)
                .dueDate(parent.getDueDate())
                .createdAt(now)
                .reporterType("Officer")
                .voiceDuration("")
                .comments(new ArrayList<>())
                .notes(new ArrayList<>())
                .activities(new ArrayList<>(List.of(DetailedActivity.builder()
                        .timestamp(Formats.dateTime(now))
                        .action("SUBTASK_CREATED")
                        .performedBy("Officer")
                        .remarks("Subtask created under parent " + parentId)
                        .build())))
                .build();

        Task saved = tasks.save(subTask);

        if (req.departmentId() != null && !req.departmentId().isBlank()) {
            TaskAssignment assignment = TaskAssignment.builder()
                    .taskId(saved.getId())
                    .departmentId(req.departmentId())
                    .officerId(req.officerId())
                    .status("PENDING")
                    .assignedAt(now)
                    .build();
            taskAssignments.save(assignment);
        }

        parent.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(now))
                .action("SUBTASK_ADDED")
                .performedBy("Officer")
                .remarks("Subtask " + saved.getId() + " added: " + req.title())
                .build());
        tasks.save(parent);

        return saved;
    }

    /**
     * Checks if there are any duplicate tickets in the same organization and close geographic area.
     * Groups them under the same groupId if a duplicate is found.
     */
    private void checkForDuplicates(Task task) {
        List<Task> existingTasks = tasks.findByOrgIdAndParentTaskIdIsNull(task.getOrgId(), org.springframework.data.domain.Pageable.unpaged());

        for (Task existing : existingTasks) {
            if (existing.getId().equals(task.getId())) {
                continue;
            }

            double distance = distanceInMeters(task.getLocation(), existing.getLocation());
            if (distance > 300.0) { // 300 meters threshold
                continue;
            }

            boolean isDuplicate = false;

            // Check embedding similarity
            if (task.getDescriptionEmbedding() != null && !task.getDescriptionEmbedding().isEmpty() &&
                    existing.getDescriptionEmbedding() != null && !existing.getDescriptionEmbedding().isEmpty()) {
                double similarity = cosineSimilarity(task.getDescriptionEmbedding(), existing.getDescriptionEmbedding());
                if (similarity >= 0.82) {
                    isDuplicate = true;
                    log.info("Detected duplicate via embedding similarity ({}): {} and {}", similarity, task.getId(), existing.getId());
                }
            }

            // Fallback word similarity check
            if (!isDuplicate) {
                if (task.getCategory() != null && task.getCategory().equalsIgnoreCase(existing.getCategory())) {
                    double textSim = wordSimilarity(
                            (task.getTitle() + " " + task.getDescription()),
                            (existing.getTitle() + " " + existing.getDescription())
                    );
                    if (textSim >= 0.35) {
                        isDuplicate = true;
                        log.info("Detected duplicate via text word similarity ({}): {} and {}", textSim, task.getId(), existing.getId());
                    }
                }
            }

            if (isDuplicate) {
                task.setGroupId(existing.getGroupId());
                
                long now = System.currentTimeMillis();
                task.getActivities().add(DetailedActivity.builder()
                        .timestamp(Formats.dateTime(now))
                        .action("DUPLICATE_GROUPED")
                        .performedBy("system_ai")
                        .remarks("Identified as duplicate of " + existing.getId() + " (Distance: " + Math.round(distance) + "m). Grouped under: " + existing.getGroupId())
                        .build());
                
                existing.getActivities().add(DetailedActivity.builder()
                        .timestamp(Formats.dateTime(now))
                        .action("DUPLICATE_LINKED")
                        .performedBy("system_ai")
                        .remarks("New ticket " + task.getId() + " identified as duplicate and grouped here (Distance: " + Math.round(distance) + "m)")
                        .build());
                tasks.save(existing);
                break; 
            }
        }
    }

    /**
     * Calculates distance between two geolocations in meters using the Haversine formula.
     */
    private double distanceInMeters(Task.TaskLocation loc1, Task.TaskLocation loc2) {
        if (loc1 == null || loc2 == null || loc1.getGeo() == null || loc2.getGeo() == null) {
            return Double.MAX_VALUE;
        }
        List<Double> coord1 = loc1.getGeo().getCoordinates();
        List<Double> coord2 = loc2.getGeo().getCoordinates();
        if (coord1 == null || coord1.size() < 2 || coord2 == null || coord2.size() < 2) {
            return Double.MAX_VALUE;
        }
        double lon1 = coord1.get(0);
        double lat1 = coord1.get(1);
        double lon2 = coord2.get(0);
        double lat2 = coord2.get(1);

        if ((lon1 == 0.0 && lat1 == 0.0) || (lon2 == 0.0 && lat2 == 0.0)) {
            return Double.MAX_VALUE;
        }

        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return earthRadius * c;
    }

    /**
     * Computes the cosine similarity between two vector embeddings.
     */
    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Computes the Jaccard similarity coefficient between words of two strings.
     */
    private double wordSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        s1 = s1.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        s2 = s2.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] w1 = s1.split("\\s+");
        String[] w2 = s2.split("\\s+");
        java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(w1));
        java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(w2));
        java.util.Set<String> stopWords = new java.util.HashSet<>(java.util.Arrays.asList("a", "the", "of", "in", "on", "at", "is", "are", "for", "to", "and", "it", "this", "that"));
        set1.removeAll(stopWords);
        set2.removeAll(stopWords);
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        
        int intersectionSize = 0;
        for (String w : set1) {
            if (set2.contains(w)) {
                intersectionSize++;
            }
        }
        int unionSize = set1.size() + set2.size() - intersectionSize;
        return (double) intersectionSize / unionSize;
    }
}

