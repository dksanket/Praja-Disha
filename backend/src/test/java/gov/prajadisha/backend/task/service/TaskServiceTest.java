package gov.prajadisha.backend.task.service;

import gov.prajadisha.backend.ai.service.AiTriageService;
import gov.prajadisha.backend.ai.service.GoogleSpeechService;
import gov.prajadisha.backend.ai.service.OllamaClient;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.GeoPolygon;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.model.Organization;
import gov.prajadisha.backend.task.model.TaskAssignment;
import gov.prajadisha.backend.org.service.OrganizationService;
import gov.prajadisha.backend.storage.StorageService;
import gov.prajadisha.backend.task.model.DetailedActivity;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskService taskService;

    @Mock
    private TaskRepository tasks;
    @Mock
    private OrganizationService organizations;
    @Mock
    private AiTriageService triage;
    @Mock
    private OllamaClient ollama;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private StorageService storageService;
    @Mock
    private GoogleSpeechService speechService;
    @Mock
    private gov.prajadisha.backend.org.repository.DepartmentRepository departments;
    @Mock
    private gov.prajadisha.backend.org.repository.OfficerRepository officers;
    @Mock
    private gov.prajadisha.backend.task.repository.TaskAssignmentRepository taskAssignments;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taskService = new TaskService(tasks, organizations, triage, ollama, events, storageService, speechService, departments, officers, taskAssignments);
    }

    @Test
    void testAutoTriage_WithVoiceMessage_TranscribesAndTranslates() throws IOException {
        // Arrange
        String taskId = "PD-123";
        Task task = Task.builder()
                .id(taskId)
                .voiceUrl("/files/audio/test.wav")
                .language("Kannada") // Gets mapped to kn-IN
                .description("Voice report submission")
                .activities(new ArrayList<>())
                .build();

        byte[] mockAudioBytes = new byte[]{1, 2, 3};
        String mockTranscript = "ರಸ್ತೆ ಗುಂಡಿ ಇದೆ"; // Kannada transcript
        String mockTranslation = "There is a road pothole"; // English translation

        Organization testOrg = Organization.builder()
                .id("ORG-101")
                .name("BBMP")
                .build();
        when(organizations.findAll()).thenReturn(List.of(testOrg));
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(storageService.loadFileBytes(task.getVoiceUrl())).thenReturn(mockAudioBytes);
        when(speechService.transcribe(mockAudioBytes, "kn-IN")).thenReturn(mockTranscript);
        when(ollama.isEnabled()).thenReturn(true);
        when(ollama.chat(any(), eq(mockTranscript))).thenReturn(mockTranslation);

        AiTriageService.Classification mockClassification = new AiTriageService.Classification(
                "Infrastructure",
                "P1",
                "Roads Department",
                "Road pothole issue",
                "English"
        );
        when(triage.classify(task)).thenReturn(mockClassification);

        // Act
        taskService.autoTriage(taskId);

        // Assert
        // Verify transcription was called
        verify(speechService).transcribe(mockAudioBytes, "kn-IN");
        // Verify translation was called
        verify(ollama).chat(contains("translator"), eq(mockTranscript));
        // Verify task description was updated with translated text
        assertEquals(mockTranslation, task.getDescription());
        // Verify task save happened
        verify(tasks).save(task);
    }

    @Test
    void testAutoTriage_WithDuplicateTaskInProximity_GroupsThem() {
        // Arrange
        String existingId = "PD-8821";
        Task existingTask = Task.builder()
                .id(existingId)
                .groupId(existingId)
                .orgId("ORG-101")
                .title("Road pothole")
                .description("There is a road pothole near the main crossing")
                .category("Infrastructure")
                .location(new Task.TaskLocation("Main Crossing", GeoPoint.of(77.5946, 12.9716)))
                .descriptionEmbedding(java.util.List.of(0.1, 0.2, 0.3))
                .activities(new ArrayList<>())
                .build();

        String newTaskId = "PD-9999";
        Task newTask = Task.builder()
                .id(newTaskId)
                .groupId(newTaskId)
                .orgId("ORG-101")
                .title("Pothole on road")
                .description("Huge pothole on road near main crossing")
                .category("Infrastructure")
                .location(new Task.TaskLocation("Main Crossing Street", GeoPoint.of(77.5947, 12.9715))) // ~15 meters away
                .activities(new ArrayList<>())
                .build();

        Organization testOrg = Organization.builder()
                .id("ORG-101")
                .name("BBMP")
                .build();
        when(organizations.findAll()).thenReturn(List.of(testOrg));
        when(tasks.findById(newTaskId)).thenReturn(Optional.of(newTask));
        when(tasks.findByOrgIdAndParentTaskIdIsNull(eq("ORG-101"), any())).thenReturn(java.util.List.of(existingTask, newTask));
        
        // Mock classification
        AiTriageService.Classification mockClassification = new AiTriageService.Classification(
                "Infrastructure", "P1", "Roads Department", "Pothole on road", "English"
        );
        when(triage.classify(newTask)).thenReturn(mockClassification);
        
        // Mock embedding output
        when(ollama.embed(anyString())).thenReturn(java.util.List.of(0.1, 0.2, 0.31));

        // Act
        taskService.autoTriage(newTaskId);

        // Assert
        // The new task should be grouped under the existing task's groupId
        assertEquals(existingId, newTask.getGroupId());
        // Verify that activities list was appended with group activity log
        boolean hasGroupActivity = newTask.getActivities().stream()
                .anyMatch(activity -> "DUPLICATE_GROUPED".equals(activity.getAction()));
        assertEquals(true, hasGroupActivity);
    }

    @Test
    void testAutoTriage_SuccessfulGeoAndDescriptionAssignment() {
        // Arrange
        String taskId = "PD-5555";
        GeoPolygon boundary = new GeoPolygon("Polygon", List.of(List.of(
                List.of(77.59, 12.97), List.of(77.62, 12.97),
                List.of(77.62, 13.01), List.of(77.59, 12.97))));
        
        Organization org = Organization.builder()
                .id("ORG-101")
                .name("BBMP")
                .description("BBMP municipal corporation")
                .constituency(new Organization.OrgConstituency("Bengaluru Central", boundary))
                .build();

        Task task = Task.builder()
                .id(taskId)
                .orgId("ORG-101")
                .title("Broken lamp")
                .description("Lamp post flickering")
                .location(new Task.TaskLocation("Main Cross", GeoPoint.of(77.5946, 12.9716)))
                .activities(new ArrayList<>())
                .build();

        Department dept = Department.builder()
                .id("DPT-001")
                .orgId("ORG-101")
                .name("Streetlights & Grid")
                .roleDescription("Maintains streetlights")
                .constituency(new Department.DepartmentConstituency("Streetlights Area", boundary))
                .build();

        Officer officer = Officer.builder()
                .id("OFF-101")
                .name("Rajesh Kumar")
                .isActive(true)
                .departmentIds(List.of("DPT-001"))
                .build();

        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(organizations.findById("ORG-101")).thenReturn(Optional.of(org));
        when(organizations.findAll()).thenReturn(List.of(org));
        when(departments.findByOrgId("ORG-101")).thenReturn(List.of(dept));
        when(departments.findById("DPT-001")).thenReturn(Optional.of(dept));
        when(officers.findByDepartmentIdsContaining("DPT-001")).thenReturn(List.of(officer));
        when(taskAssignments.countByOfficerIdAndStatusIn(eq("OFF-101"), any())).thenReturn(0L);
        when(ollama.isEnabled()).thenReturn(false); // test fallback flow

        AiTriageService.Classification mockClassification = new AiTriageService.Classification(
                "Infrastructure", "P1", "Streetlights & Grid", "Broken lamp", "English"
        );
        when(triage.classify(task)).thenReturn(mockClassification);

        // Act
        taskService.autoTriage(taskId);

        // Assert
        // Verify assignment saved
        ArgumentCaptor<TaskAssignment> captor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignments).save(captor.capture());
        TaskAssignment assignment = captor.getValue();
        assertEquals("DPT-001", assignment.getDepartmentId());
        assertEquals("OFF-101", assignment.getOfficerId());
        
        // Verify activity logged
        boolean hasAssignmentActivity = task.getActivities().stream()
                .anyMatch(activity -> "AI_ASSIGNED".equals(activity.getAction()) && 
                        activity.getRemarks().contains("assigned to department 'Streetlights & Grid'"));
        assertTrue(hasAssignmentActivity);
    }

    @Test
    void testAutoTriage_OrganizationCoordinateMismatch_AssignmentFails() {
        // Arrange
        String taskId = "PD-5555";
        GeoPolygon boundary = new GeoPolygon("Polygon", List.of(List.of(
                List.of(77.59, 12.97), List.of(77.62, 12.97),
                List.of(77.62, 13.01), List.of(77.59, 12.97))));
        
        Organization org = Organization.builder()
                .id("ORG-101")
                .name("BBMP")
                .constituency(new Organization.OrgConstituency("Bengaluru Central", boundary))
                .build();

        Task task = Task.builder()
                .id(taskId)
                .orgId("ORG-101")
                .title("Broken lamp")
                .description("Lamp post flickering")
                // Far outside the boundary
                .location(new Task.TaskLocation("Out of town", GeoPoint.of(12.0, 12.0)))
                .activities(new ArrayList<>())
                .build();

        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(organizations.findById("ORG-101")).thenReturn(Optional.of(org));
        when(organizations.findAll()).thenReturn(List.of(org));
        
        AiTriageService.Classification mockClassification = new AiTriageService.Classification(
                "Infrastructure", "P1", "Streetlights & Grid", "Broken lamp", "English"
        );
        when(triage.classify(task)).thenReturn(mockClassification);

        // Act
        taskService.autoTriage(taskId);

        // Assert
        // Verify assignment NOT saved
        verify(taskAssignments, never()).save(any());
        
        // Verify failed activity logged
        boolean hasFailedActivity = task.getActivities().stream()
                .anyMatch(activity -> "ASSIGNMENT_FAILED".equals(activity.getAction()) &&
                        activity.getRemarks().contains("outside the constituency"));
        assertTrue(hasFailedActivity);
    }
}
