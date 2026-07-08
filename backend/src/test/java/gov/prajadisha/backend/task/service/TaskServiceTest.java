package gov.prajadisha.backend.task.service;

import gov.prajadisha.backend.ai.service.AiTriageService;
import gov.prajadisha.backend.ai.service.GoogleSpeechService;
import gov.prajadisha.backend.ai.service.OllamaClient;
import gov.prajadisha.backend.common.GeoPoint;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}
