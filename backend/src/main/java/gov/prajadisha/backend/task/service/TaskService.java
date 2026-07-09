package gov.prajadisha.backend.task.service;

import gov.prajadisha.backend.ai.service.AiTriageService;
import gov.prajadisha.backend.ai.service.GoogleSpeechService;
import gov.prajadisha.backend.ai.service.OllamaClient;
import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Formats;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.GeoPolygon;
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
import gov.prajadisha.backend.org.model.Organization;
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

            // Resolve Organization based on geocoordinates and/or description
            Organization resolvedOrg = resolveOrganization(task);
            if (resolvedOrg == null) {
                log.warn("Auto-assignment failed: No matching organization found for task {}", task.getId());
                task.getActivities().add(DetailedActivity.builder()
                        .timestamp(Formats.dateTime(System.currentTimeMillis()))
                        .action("ASSIGNMENT_FAILED")
                        .performedBy("system_ai")
                        .remarks("Auto-assignment failed: No suitable organization found matching location or description.")
                        .build());
                tasks.save(task);
                return;
            }

            task.setOrgId(resolvedOrg.getId());

            // Log organization routing activity
            boolean matchedByGeo = false;
            if (task.getLocation() != null && task.getLocation().getGeo() != null && task.getLocation().getGeo().getCoordinates() != null && task.getLocation().getGeo().getCoordinates().size() >= 2) {
                double lng = task.getLocation().getGeo().getCoordinates().get(0);
                double lat = task.getLocation().getGeo().getCoordinates().get(1);
                if (resolvedOrg.getConstituency() != null && resolvedOrg.getConstituency().getCoordinates() != null) {
                    matchedByGeo = isPointInPolygon(lng, lat, resolvedOrg.getConstituency().getCoordinates());
                }
            }

            task.getActivities().add(DetailedActivity.builder()
                    .timestamp(Formats.dateTime(System.currentTimeMillis()))
                    .action("ORG_ROUTED")
                    .performedBy("system_ai")
                    .remarks("Task automatically routed to organization '" + resolvedOrg.getName() + "'"
                            + (matchedByGeo ? " based on constituency geographic coordinates." : " based on content description match."))
                    .build());

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

            // Verify resolved organization boundary
            boolean isInBoundary = true;
            if (resolvedOrg.getConstituency() != null && resolvedOrg.getConstituency().getCoordinates() != null) {
                if (task.getLocation() != null && task.getLocation().getGeo() != null && task.getLocation().getGeo().getCoordinates() != null && task.getLocation().getGeo().getCoordinates().size() >= 2) {
                    double lng = task.getLocation().getGeo().getCoordinates().get(0);
                    double lat = task.getLocation().getGeo().getCoordinates().get(1);
                    if (!isPointInPolygon(lng, lat, resolvedOrg.getConstituency().getCoordinates())) {
                        isInBoundary = false;
                        log.warn("Auto-assignment failed: Task coordinates [{}, {}] are outside the resolved constituency '{}' boundary.", lng, lat, resolvedOrg.getConstituency().getName());
                        task.getActivities().add(DetailedActivity.builder()
                                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                                .action("ASSIGNMENT_FAILED")
                                .performedBy("system_ai")
                                .remarks("Auto-assignment failed: Task coordinates [" + lng + ", " + lat + "] are outside the constituency '" + resolvedOrg.getConstituency().getName() + "' boundary.")
                                .build());
                    }
                }
            }

            Department targetDept = null;
            Officer assignedOfficer = null;

            if (isInBoundary) {
                // Route to Department based on coordinates and description
                try {
                    List<Department> allDepts = departments.findByOrgId(task.getOrgId());
                    
                    // Filter departments by coordinates
                    List<Department> geoCandidates = new ArrayList<>();
                    for (Department dept : allDepts) {
                        if (dept.getConstituency() != null && dept.getConstituency().getCoordinates() != null) {
                            if (task.getLocation() != null && task.getLocation().getGeo() != null && task.getLocation().getGeo().getCoordinates() != null && task.getLocation().getGeo().getCoordinates().size() >= 2) {
                                double lng = task.getLocation().getGeo().getCoordinates().get(0);
                                double lat = task.getLocation().getGeo().getCoordinates().get(1);
                                if (isPointInPolygon(lng, lat, dept.getConstituency().getCoordinates())) {
                                    geoCandidates.add(dept);
                                }
                            }
                        } else {
                            geoCandidates.add(dept); // Global department
                        }
                    }

                    if (!geoCandidates.isEmpty()) {
                        if (ollama.isEnabled()) {
                            targetDept = selectBestDepartmentWithAi(task, geoCandidates);
                        }
                        if (targetDept == null) {
                            targetDept = selectBestDepartmentFallback(task, geoCandidates, c.suggestedDepartment());
                        }
                    }

                    if (targetDept != null) {
                        // Find the least loaded active officer in the department
                        List<Officer> deptOfficers = officers.findByDepartmentIdsContaining(targetDept.getId());
                        List<Officer> activeOfficers = deptOfficers.stream()
                                .filter(Officer::isActive)
                                .toList();
                        
                        if (!activeOfficers.isEmpty()) {
                            Officer leastLoaded = null;
                            long minLoad = Long.MAX_VALUE;
                            for (Officer off : activeOfficers) {
                                long load = getOfficerWorkload(off.getId());
                                if (load < minLoad) {
                                    minLoad = load;
                                    leastLoaded = off;
                                }
                            }
                            assignedOfficer = leastLoaded;
                        }

                        // Fallback to department's head officer if no active officers or load-balancing failed
                        if (assignedOfficer == null && targetDept.getHeadOfficerId() != null) {
                            assignedOfficer = officers.findById(targetDept.getHeadOfficerId()).orElse(null);
                        }

                        // Assign task to the entire department hierarchy (from root down to leaf)
                        saveHierarchyAssignments(task.getId(), targetDept.getId(), assignedOfficer != null ? assignedOfficer.getId() : null, System.currentTimeMillis());


                        task.getActivities().add(DetailedActivity.builder()
                                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                                .action("AI_ASSIGNED")
                                .performedBy("system_ai")
                                .remarks("Auto-classified and assigned to department '" + targetDept.getName() + "'"
                                        + (assignedOfficer != null ? " and officer '" + assignedOfficer.getName() + "'" : ""))
                                .build());
                    } else {
                        task.getActivities().add(DetailedActivity.builder()
                                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                                .action("ASSIGNMENT_FAILED")
                                .performedBy("system_ai")
                                .remarks("Auto-assignment failed: No suitable department found based on location/description.")
                                .build());
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-assign task {} to department/officer", task.getId(), e);
                }
            }

            // Generate rich vector embedding using all task details and verify duplicates
            generateEmbeddingAndCheckDuplicates(task, targetDept, assignedOfficer);

            tasks.save(task);
        });
    }

    /**
     * Constructs a detailed rich text block representing the task, gets its embedding from Ollama,
     * and triggers semantic duplicate checking.
     */
    private void generateEmbeddingAndCheckDuplicates(Task task, Department targetDept, Officer assignedOfficer) {
        StringBuilder embedText = new StringBuilder();
        embedText.append("Title: ").append(task.getTitle() != null ? task.getTitle() : "").append("\n");
        embedText.append("Description: ").append(task.getDescription() != null ? task.getDescription() : "").append("\n");
        embedText.append("Category: ").append(task.getCategory() != null ? task.getCategory() : "").append("\n");
        embedText.append("Priority: ").append(task.getPriority() != null ? task.getPriority() : "").append("\n");
        embedText.append("Reporter: ").append(task.getReporterType() != null ? task.getReporterType() : "").append("\n");
        if (task.getLocation() != null && task.getLocation().getAddress() != null && !task.getLocation().getAddress().isBlank()) {
            embedText.append("Location: ").append(task.getLocation().getAddress()).append("\n");
        }
        if (task.getLanguage() != null && !task.getLanguage().isBlank()) {
            embedText.append("Language: ").append(task.getLanguage()).append("\n");
        }
        if (targetDept != null) {
            embedText.append("Department: ").append(targetDept.getName()).append("\n");
        }
        if (assignedOfficer != null) {
            embedText.append("Officer: ").append(assignedOfficer.getName()).append("\n");
        }

        String content = embedText.toString().trim();
        log.info("Generating rich details embedding for task {} with content:\n{}", task.getId(), content);

        List<Double> embedding = ollama.embed(content);
        if (!embedding.isEmpty()) {
            task.setDescriptionEmbedding(embedding);
        }

        try {
            checkForDuplicates(task);
        } catch (Exception e) {
            log.error("Error during duplicate checking for task: {}", task.getId(), e);
        }
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
        // Fetch all assignments in the hierarchy sorted from root to leaf
        List<TaskAssignment> assigns = getSortedAssignmentsForTask(t.getId());
        if (assigns != null && !assigns.isEmpty()) {
            for (TaskAssignment a : assigns) {
                departments.findById(a.getDepartmentId()).ifPresent(dept -> {
                    assignments.add(Assignment.dept(dept.getName()));
                });
            }
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

        List<Task> sub = tasks.findByParentTaskId(t.getId());
        if (sub == null) {
            sub = new ArrayList<>();
        }

        List<gov.prajadisha.backend.task.dto.TaskDtos.SubTask> mappedSubTasks = new ArrayList<>();

        // 1. Add the department hierarchy chain of the root task as tree nodes
        List<TaskAssignment> rootAssigns = getSortedAssignmentsForTask(t.getId());
        if (rootAssigns == null || rootAssigns.isEmpty()) {
            // Fallback to task category if no assignments are found
            mappedSubTasks.add(new gov.prajadisha.backend.task.dto.TaskDtos.SubTask(
                    t.getId(),
                    null,
                    t.getTitle(),
                    "Root Task",
                    t.getCategory(),
                    "account_tree",
                    "ACTIVE",
                    "bg-primary-container text-on-primary-container",
                    "",
                    ""
            ));
        } else {
            for (int i = 0; i < rootAssigns.size(); i++) {
                TaskAssignment assign = rootAssigns.get(i);
                Department dept = departments.findById(assign.getDepartmentId()).orElse(null);
                if (dept == null) continue;
                
                String deptName = dept.getName();
                String assignee = "";
                if (assign.getOfficerId() != null && !assign.getOfficerId().isBlank()) {
                    assignee = officers.findById(assign.getOfficerId())
                            .map(gov.prajadisha.backend.org.model.Officer::getName)
                            .orElse("");
                }
                
                String role = "Division";
                if (i == 0) {
                    role = "Root Division";
                } else if (i == rootAssigns.size() - 1) {
                    role = "Root Task"; // Matches frontend filter to identify the leaf task node
                } else {
                    role = "Sub-Division";
                }
                
                String nodeId;
                String parentNodeId;
                if (i == 0) {
                    nodeId = t.getId() + "-root";
                    parentNodeId = null;
                } else if (i == rootAssigns.size() - 1) {
                    nodeId = t.getId(); // Leaf ID matches t.getId() so actual subtasks nest under it
                    parentNodeId = t.getId() + (i - 1 == 0 ? "-root" : "-level-" + (i - 1));
                } else {
                    nodeId = t.getId() + "-level-" + i;
                    parentNodeId = t.getId() + (i - 1 == 0 ? "-root" : "-level-" + (i - 1));
                }
                
                mappedSubTasks.add(new gov.prajadisha.backend.task.dto.TaskDtos.SubTask(
                        nodeId,
                        parentNodeId,
                        t.getTitle(),
                        role,
                        deptName,
                        "account_tree",
                        "ACTIVE",
                        "bg-primary-container text-on-primary-container",
                        assignee,
                        ""
                ));
            }
        }

        // 2. Add each actual subtask
        for (Task subT : sub) {
            String deptName = "";
            String officerName = "";
            String roleStr = "Unassigned";

            try {
                // Fetch assignments sorted from root to leaf and pick the leaf assignment details
                List<TaskAssignment> assigns = getSortedAssignmentsForTask(subT.getId());
                if (assigns != null && !assigns.isEmpty()) {
                    TaskAssignment assign = assigns.get(assigns.size() - 1);
                    gov.prajadisha.backend.org.model.Department dept = departments.findById(assign.getDepartmentId()).orElse(null);
                    if (dept != null) {
                        deptName = dept.getName();
                    }
                    if (assign.getOfficerId() != null && !assign.getOfficerId().isBlank()) {
                        gov.prajadisha.backend.org.model.Officer officer = officers.findById(assign.getOfficerId()).orElse(null);
                        if (officer != null) {
                            officerName = officer.getName();
                            if (dept != null) {
                                roleStr = "Officer: " + officer.getName() + " (" + dept.getName() + ")";
                            } else {
                                roleStr = "Officer: " + officer.getName();
                            }
                        } else {
                            if (dept != null) {
                                roleStr = "Assigned to " + dept.getName();
                            }
                        }
                    } else {
                        if (dept != null) {
                            roleStr = "Assigned to " + dept.getName();
                        }
                    }
                } else {
                    deptName = subT.getCategory() != null ? subT.getCategory() : "";
                    roleStr = "Unassigned";
                }
            } catch (Exception ex) {
                log.error("Failed to fetch assignment details for subtask: {}", subT.getId(), ex);
            }

            // Choose icon
            String titleLower = subT.getTitle() != null ? subT.getTitle().toLowerCase() : "";
            String icon = "engineering"; // default
            if (titleLower.contains("traffic") || titleLower.contains("police")) {
                icon = "traffic";
            } else if (titleLower.contains("urgent") || titleLower.contains("hazard") || titleLower.contains("emergency") || titleLower.contains("warning")) {
                icon = "warning";
            } else if (titleLower.contains("construction") || titleLower.contains("repair") || titleLower.contains("road") || titleLower.contains("grid") || titleLower.contains("line")) {
                icon = "construction";
            }

            // Map status
            String statusVal = subT.getGlobalStatus() != null ? subT.getGlobalStatus() : "Pending";
            String statusUpper = statusVal.toUpperCase().replace(" ", "_");
            String statusClass = "bg-surface-container-high text-on-surface-variant"; // default
            if ("IN_PROGRESS".equals(statusUpper)) {
                statusClass = "bg-secondary-container text-on-secondary-container";
            } else if ("RESOLVED".equals(statusUpper)) {
                statusClass = "bg-success-container text-on-success-container";
            } else if ("ACTIVE".equals(statusUpper) || "SUBMITTED".equals(statusUpper)) {
                statusClass = "bg-primary-container text-on-primary-container";
            }

            mappedSubTasks.add(new gov.prajadisha.backend.task.dto.TaskDtos.SubTask(
                    subT.getId(),
                    t.getId(), // parentId in frontend maps to root task id
                    subT.getTitle(),
                    roleStr,
                    deptName,
                    icon,
                    statusUpper,
                    statusClass,
                    officerName,
                    Formats.dateTime(subT.getCreatedAt())
            ));
        }

        return new TaskDetailPayload(
                t.getId(),
                t.getTitle(),
                t.getPriority(),
                t.getGroupId(),
                t.getParentTaskId(),
                t.getOrgId(),
                t.getGlobalStatus() == null ? "Submitted" : t.getGlobalStatus(),
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
                mappedSubTasks,
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

    public TaskDetailPayload updateAssignee(String id, gov.prajadisha.backend.task.dto.TaskDtos.UpdateAssigneeRequest req) {
        // Save the complete hierarchy of departments for this reassignment
        saveHierarchyAssignments(id, req.departmentId(), req.officerId(), System.currentTimeMillis());
        
        Task task = get(id);
        
        if (req.departmentId() != null && !req.departmentId().isBlank()) {
            String newCategory = departments.findById(req.departmentId())
                    .map(gov.prajadisha.backend.org.model.Department::getName)
                    .orElse(task.getCategory());
            task.setCategory(newCategory);
        }
        
        String deptName = req.departmentId() != null && !req.departmentId().isBlank() ? 
            departments.findById(req.departmentId()).map(gov.prajadisha.backend.org.model.Department::getName).orElse("None") : "None";
        String officerName = req.officerId() != null && !req.officerId().isBlank() ? 
            officers.findById(req.officerId()).map(gov.prajadisha.backend.org.model.Officer::getName).orElse("None") : "None";
            
        task.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                .action("ASSIGNEE_CHANGED")
                .performedBy("Officer")
                .remarks("Reassigned to Department: " + deptName + ", Officer: " + officerName)
                .build());
        tasks.save(task);
        return detail(id);
    }

    public TaskDetailPayload updateStatus(String id, gov.prajadisha.backend.task.dto.TaskDtos.UpdateStatusRequest req) {
        Task task = get(id);
        String oldStatus = task.getGlobalStatus();
        task.setGlobalStatus(req.status());
        if ("Resolved".equalsIgnoreCase(req.status()) || "Rejected".equalsIgnoreCase(req.status())) {
            task.setReviewed(true);
        }
        
        task.getActivities().add(DetailedActivity.builder()
                .timestamp(Formats.dateTime(System.currentTimeMillis()))
                .action("STATUS_CHANGED")
                .performedBy("Officer")
                .remarks("Status changed from '" + oldStatus + "' to '" + req.status() + "'." 
                    + (req.remarks() != null && !req.remarks().isBlank() ? " Remarks: " + req.remarks() : ""))
                .build());
        tasks.save(task);
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
            // Retrieve first organization from db dynamically
            return organizations.findAll().stream()
                    .findFirst()
                    .map(gov.prajadisha.backend.org.model.Organization::getId)
                    .orElseThrow(() -> e);
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
            // Save the complete hierarchy of departments for the subtask
            saveHierarchyAssignments(saved.getId(), req.departmentId(), req.officerId(), now);
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
            if (distance > 500.0) { // 500 meters threshold
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
                boolean isCategoryMatch = isCategorySimilar(task.getCategory(), existing.getCategory());
                double textSim = wordSimilarity(
                        (task.getTitle() + " " + task.getDescription()),
                        (existing.getTitle() + " " + existing.getDescription())
                );
                
                // If same area and high text similarity (>= 0.30 with category match, or >= 0.45 without)
                if ((isCategoryMatch && textSim >= 0.30) || textSim >= 0.45) {
                    isDuplicate = true;
                    log.info("Detected duplicate via text word similarity ({}): {} and {}", textSim, task.getId(), existing.getId());
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
     * Checks if two categories are similar (either exactly match, or one contains/starts with the other).
     */
    private boolean isCategorySimilar(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        String c1 = cat1.toLowerCase().trim();
        String c2 = cat2.toLowerCase().trim();
        return c1.equals(c2) || c1.contains(c2) || c2.contains(c1);
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

    /**
     * Checks if coordinates fall inside the GeoPolygon using the standard ray-casting algorithm.
     */
    private static boolean isPointInPolygon(double longitude, double latitude, GeoPolygon polygon) {
        if (polygon == null || polygon.getCoordinates() == null || polygon.getCoordinates().isEmpty()) {
            return false;
        }
        List<List<Double>> exteriorRing = polygon.getCoordinates().get(0);
        if (exteriorRing == null || exteriorRing.size() < 3) {
            return false;
        }

        boolean inside = false;
        int n = exteriorRing.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            List<Double> pi = exteriorRing.get(i);
            List<Double> pj = exteriorRing.get(j);
            if (pi == null || pi.size() < 2 || pj == null || pj.size() < 2) {
                continue;
            }
            double xi = pi.get(0);
            double yi = pi.get(1);
            double xj = pj.get(0);
            double yj = pj.get(1);

            boolean intersect = ((yi > latitude) != (yj > latitude))
                    && (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }

        if (inside && polygon.getCoordinates().size() > 1) {
            for (int h = 1; h < polygon.getCoordinates().size(); h++) {
                List<List<Double>> hole = polygon.getCoordinates().get(h);
                if (hole == null || hole.size() < 3) {
                    continue;
                }
                boolean inHole = false;
                int hn = hole.size();
                for (int i = 0, j = hn - 1; i < hn; j = i++) {
                    List<Double> pi = hole.get(i);
                    List<Double> pj = hole.get(j);
                    if (pi == null || pi.size() < 2 || pj == null || pj.size() < 2) {
                        continue;
                    }
                    double xi = pi.get(0);
                    double yi = pi.get(1);
                    double xj = pj.get(0);
                    double yj = pj.get(1);

                    boolean intersect = ((yi > latitude) != (yj > latitude))
                            && (longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi);
                    if (intersect) {
                        inHole = !inHole;
                    }
                }
                if (inHole) {
                    return false;
                }
            }
        }

        return inside;
    }

    /**
     * Uses Ollama to check if a task matches the organization's jurisdiction description.
     */
    private boolean checkTaskMatchesDescriptionWithAiOrFallback(Task task, String description) {
        if (ollama.isEnabled()) {
            try {
                String systemPrompt = "You are a municipal triage validator. You must determine if a civic task/complaint falls under the jurisdiction of a specific organization based on its description.\n" +
                        "Organization Description: " + description + "\n" +
                        "Respond with ONLY 'yes' or 'no'. Do not include any other text, explanation, or punctuation.";
                String userPrompt = "Task Title: " + (task.getTitle() == null ? "" : task.getTitle()) + "\n" +
                        "Task Description: " + (task.getDescription() == null ? "" : task.getDescription());
                String response = ollama.chat(systemPrompt, userPrompt);
                if (response != null) {
                    String trimmed = response.trim().toLowerCase();
                    if (trimmed.contains("yes")) {
                        return true;
                    } else if (trimmed.contains("no")) {
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("AI check for organization description match failed: {}", e.getMessage());
            }
        }
        return true;
    }

    /**
     * Uses Ollama to select the best matching candidate department based on role description.
     */
    private Department selectBestDepartmentWithAi(Task task, List<Department> candidates) {
        try {
            StringBuilder deptBlock = new StringBuilder();
            for (Department dept : candidates) {
                deptBlock.append("- ID: ").append(dept.getId())
                         .append(", Name: ").append(dept.getName())
                         .append(", Role: ").append(dept.getRoleDescription() == null ? "" : dept.getRoleDescription())
                         .append("\n");
            }

            String systemPrompt = "You are a municipal routing assistant. Based on the civic ticket below and the list of candidate departments, select the best matching department based on their role description.\n" +
                    "Candidate Departments:\n" +
                    deptBlock + "\n" +
                    "Respond with ONLY the ID of the chosen department. If none of the departments are appropriate, respond with 'NONE'. Do not include any other text, explanation, or punctuation.";
            String userPrompt = "Task Title: " + (task.getTitle() == null ? "" : task.getTitle()) + "\n" +
                    "Task Description: " + (task.getDescription() == null ? "" : task.getDescription());
            String response = ollama.chat(systemPrompt, userPrompt);
            if (response != null) {
                String chosenId = response.trim();
                if (chosenId.contains("\n")) {
                    chosenId = chosenId.split("\n")[0];
                }
                chosenId = chosenId.replaceAll("[^a-zA-Z0-9-]", "");
                if ("NONE".equalsIgnoreCase(chosenId)) {
                    return null;
                }
                String finalId = chosenId;
                return candidates.stream()
                        .filter(d -> finalId.equalsIgnoreCase(d.getId()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("AI department selection failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fallback department matching based on name and role description keywords.
     */
    private Department selectBestDepartmentFallback(Task task, List<Department> candidates, String suggestedDepartmentName) {
        if (suggestedDepartmentName != null && !suggestedDepartmentName.isBlank()) {
            Optional<Department> match = candidates.stream()
                    .filter(d -> suggestedDepartmentName.equalsIgnoreCase(d.getName()))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }
        String text = ((task.getTitle() == null ? "" : task.getTitle()) + " " +
                       (task.getDescription() == null ? "" : task.getDescription())).toLowerCase();
        for (Department d : candidates) {
            String dName = d.getName().toLowerCase();
            if (text.contains(dName)) {
                return d;
            }
            if (d.getRoleDescription() != null) {
                String dRole = d.getRoleDescription().toLowerCase();
                if (text.contains(dRole) || containsKeywords(text, dName)) {
                    return d;
                }
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean containsKeywords(String text, String name) {
        String[] words = name.split("\\s+");
        for (String w : words) {
            if (w.length() > 3 && text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts the workload (pending or in-progress assignments) for an officer.
     */
    private long getOfficerWorkload(String officerId) {
        try {
            return taskAssignments.countByOfficerIdAndStatusIn(officerId, List.of("PENDING", "IN_PROGRESS"));
        } catch (Exception e) {
            log.warn("Failed to count workload for officer {}: {}", officerId, e.getMessage());
            return 0;
        }
    }

    /**
     * Resolves the correct organization for a task based on geocoordinates and description.
     */
    private Organization resolveOrganization(Task task) {
        List<Organization> allOrgs;
        try {
            allOrgs = organizations.findAll();
        } catch (Exception e) {
            log.error("Failed to fetch all organizations from repository", e);
            return null;
        }
        if (allOrgs == null || allOrgs.isEmpty()) {
            log.warn("No organizations found in database to route task {}", task.getId());
            return null;
        }

        // 1. Try matching by coordinates first
        if (task.getLocation() != null && task.getLocation().getGeo() != null && task.getLocation().getGeo().getCoordinates() != null && task.getLocation().getGeo().getCoordinates().size() >= 2) {
            double lng = task.getLocation().getGeo().getCoordinates().get(0);
            double lat = task.getLocation().getGeo().getCoordinates().get(1);
            List<Organization> geoCandidates = new ArrayList<>();
            for (Organization org : allOrgs) {
                if (org.getConstituency() != null && org.getConstituency().getCoordinates() != null) {
                    if (isPointInPolygon(lng, lat, org.getConstituency().getCoordinates())) {
                        geoCandidates.add(org);
                    }
                }
            }
            if (geoCandidates.size() == 1) {
                log.info("Resolved organization {} by geocoordinates for task {}", geoCandidates.get(0).getId(), task.getId());
                return geoCandidates.get(0);
            } else if (geoCandidates.size() > 1) {
                log.info("Multiple geographic organization matches ({} found) for task {}. Selecting via AI.", geoCandidates.size(), task.getId());
                Organization best = selectBestOrganizationWithAi(task, geoCandidates);
                if (best != null) {
                    return best;
                }
                return geoCandidates.get(0);
            }
        }

        // 2. If no coordinates match (or missing coordinates), match using AI based on description
        log.info("No geocoordinate match found for task {}. Attempting description-based AI matching.", task.getId());
        Organization best = selectBestOrganizationWithAi(task, allOrgs);
        if (best != null) {
            return best;
        }

        // 3. Fallback keyword matching
        log.info("AI organization matching failed or disabled. Falling back to keyword matching for task {}", task.getId());
        return selectBestOrganizationFallback(task, allOrgs);
    }

    /**
     * Uses Ollama to select the best matching candidate organization based on its description.
     */
    private Organization selectBestOrganizationWithAi(Task task, List<Organization> candidates) {
        if (!ollama.isEnabled()) {
            return null;
        }
        try {
            StringBuilder orgBlock = new StringBuilder();
            for (Organization org : candidates) {
                orgBlock.append("- ID: ").append(org.getId())
                         .append(", Name: ").append(org.getName())
                         .append(", Description: ").append(org.getDescription() == null ? "" : org.getDescription())
                         .append("\n");
            }

            String systemPrompt = "You are a municipal organization routing assistant. Based on the civic ticket below and the list of candidate organizations, select the best matching organization whose jurisdiction or description aligns with the ticket.\n" +
                    "Candidate Organizations:\n" +
                    orgBlock + "\n" +
                    "Respond with ONLY the ID of the chosen organization. If none of the organizations are appropriate, respond with 'NONE'. Do not include any other text, explanation, or punctuation.";
            String userPrompt = "Task Title: " + (task.getTitle() == null ? "" : task.getTitle()) + "\n" +
                    "Task Description: " + (task.getDescription() == null ? "" : task.getDescription());
            String response = ollama.chat(systemPrompt, userPrompt);
            if (response != null) {
                String chosenId = response.trim();
                if (chosenId.contains("\n")) {
                    chosenId = chosenId.split("\n")[0];
                }
                chosenId = chosenId.replaceAll("[^a-zA-Z0-9-]", "");
                if ("NONE".equalsIgnoreCase(chosenId)) {
                    return null;
                }
                String finalId = chosenId;
                return candidates.stream()
                        .filter(org -> finalId.equalsIgnoreCase(org.getId()))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("AI organization selection failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fallback organization matching based on name and description keywords.
     */
    private Organization selectBestOrganizationFallback(Task task, List<Organization> candidates) {
        String text = ((task.getTitle() == null ? "" : task.getTitle()) + " " +
                       (task.getDescription() == null ? "" : task.getDescription())).toLowerCase();
        for (Organization org : candidates) {
            String orgName = org.getName().toLowerCase();
            if (text.contains(orgName)) {
                return org;
            }
            if (org.getDescription() != null) {
                String orgDesc = org.getDescription().toLowerCase();
                if (text.contains(orgDesc) || containsKeywords(text, orgName)) {
                    return org;
                }
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Resolves the full department hierarchy from a given target department ID
     * up to the root, creating TaskAssignment records for each tier.
     */
    private void saveHierarchyAssignments(String taskId, String targetDeptId, String targetOfficerId, long now) {
        taskAssignments.deleteByTaskId(taskId);
        if (targetDeptId == null || targetDeptId.isBlank()) {
            return;
        }

        List<Department> hierarchy = new ArrayList<>();
        String currentDeptId = targetDeptId;
        while (currentDeptId != null && !currentDeptId.isBlank()) {
            Optional<Department> optDept = departments.findById(currentDeptId);
            if (optDept.isPresent()) {
                Department dept = optDept.get();
                // Add at start of list so root/parent departments come first, leaf last
                hierarchy.add(0, dept);
                currentDeptId = dept.getParentDepartmentId();
            } else {
                break;
            }
        }

        for (Department dept : hierarchy) {
            String officerId = null;
            if (dept.getId().equals(targetDeptId)) {
                // Leaf department gets the specifically assigned officer
                officerId = targetOfficerId;
            } else {
                // Parent departments default to their respective head officers
                officerId = dept.getHeadOfficerId();
            }
            TaskAssignment assignment = TaskAssignment.builder()
                    .taskId(taskId)
                    .departmentId(dept.getId())
                    .officerId(officerId)
                    .status("PENDING")
                    .assignedAt(now)
                    .build();
            taskAssignments.save(assignment);
        }
    }

    /**
     * Retrieves all TaskAssignment records for a given task, sorted from
     * root to leaf based on their department hierarchy depth.
     */
    private List<TaskAssignment> getSortedAssignmentsForTask(String taskId) {
        List<TaskAssignment> list = taskAssignments.findByTaskId(taskId);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        java.util.Map<String, Integer> deptDepths = new java.util.HashMap<>();
        for (TaskAssignment a : list) {
            departments.findById(a.getDepartmentId()).ifPresent(d -> {
                deptDepths.put(d.getId(), d.getDepth() != null ? d.getDepth() : 0);
            });
        }
        
        // Sort ascending so root department (depth 0) is first, leaf is last
        list.sort((a1, a2) -> {
            int d1 = deptDepths.getOrDefault(a1.getDepartmentId(), 0);
            int d2 = deptDepths.getOrDefault(a2.getDepartmentId(), 0);
            return Integer.compare(d1, d2);
        });
        return list;
    }
}

