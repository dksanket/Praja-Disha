package gov.prajadisha.backend.config;

import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.PointActivity;
import gov.prajadisha.backend.citizen.repository.CitizenProfileRepository;
import gov.prajadisha.backend.citizen.repository.PointActivityRepository;
import gov.prajadisha.backend.common.GeoPoint;
import gov.prajadisha.backend.common.GeoPolygon;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.model.Organization;
import gov.prajadisha.backend.org.model.OrganizationConfig;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import gov.prajadisha.backend.org.repository.OrganizationConfigRepository;
import gov.prajadisha.backend.org.repository.OrganizationRepository;
import gov.prajadisha.backend.task.model.DetailedActivity;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seeds a demo organization, config, departments, officers, a citizen, and a couple of tickets
 * so both frontends have data to render on first boot. Runs only when collections are empty.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final String ORG_ID = "ORG-101";

    private final OrganizationRepository organizations;
    private final OrganizationConfigRepository orgConfigs;
    private final DepartmentRepository departments;
    private final OfficerRepository officers;
    private final CitizenProfileRepository citizens;
    private final PointActivityRepository activities;
    private final TaskRepository tasks;

    public DataSeeder(OrganizationRepository organizations, OrganizationConfigRepository orgConfigs,
                      DepartmentRepository departments, OfficerRepository officers,
                      CitizenProfileRepository citizens, PointActivityRepository activities,
                      TaskRepository tasks) {
        this.organizations = organizations;
        this.orgConfigs = orgConfigs;
        this.departments = departments;
        this.officers = officers;
        this.citizens = citizens;
        this.activities = activities;
        this.tasks = tasks;
    }

    @Override
    public void run(String... args) {
        if (organizations.count() > 0) {
            return; // already seeded
        }
        seedOrganization();
        seedConfig();
        seedDepartments();
        seedOfficers();
        seedCitizen();
        seedTasks();
    }

    private void seedOrganization() {
        GeoPolygon boundary = new GeoPolygon("Polygon", List.of(List.of(
                List.of(77.59, 12.97), List.of(77.62, 12.97),
                List.of(77.62, 13.01), List.of(77.59, 12.97))));
        organizations.save(Organization.builder()
                .id(ORG_ID)
                .name("Bruhat Bengaluru Mahanagara Palike")
                .createdAt(1791244800000L)
                .constituency(new Organization.OrgConstituency("Bengaluru Central", boundary))
                .build());
    }

    private void seedConfig() {
        orgConfigs.save(OrganizationConfig.builder()
                .orgId(ORG_ID)
                .categories(List.of(
                        new OrganizationConfig.OrgCategory("INFRASTRUCTURE", "Infrastructure",
                                "Roads, streetlights, bridges and public works"),
                        new OrganizationConfig.OrgCategory("SANITATION", "Sanitation",
                                "Garbage, drainage, sewage and public hygiene"),
                        new OrganizationConfig.OrgCategory("GRIEVANCE", "Grievance",
                                "General citizen grievances and complaints")))
                .priorities(List.of("P0", "P1", "P2", "P3"))
                .statuses(List.of("OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"))
                .updatedAt(System.currentTimeMillis())
                .customPromptExtension("Prioritise public-safety issues (electrical, structural) as P0.")
                .build());
    }

    private void seedDepartments() {
        departments.save(Department.builder()
                .id("DPT-ROOT")
                .orgId(ORG_ID)
                .name("Electrical Department")
                .parentDepartmentId(null)
                .depth(0)
                .officerCount(0)
                .roleDescription("Oversees electrical infrastructure across the constituency.")
                .build());
        departments.save(Department.builder()
                .id("DPT-001")
                .orgId(ORG_ID)
                .name("Streetlights & Grid")
                .parentDepartmentId("DPT-ROOT")
                .parentDepartmentName("Electrical Department")
                .headOfficerId("OFF-101")
                .headOfficerName("Rajesh Kumar")
                .officerCount(1)
                .depth(1)
                .roleDescription("Maintains streetlights and low-tension distribution.")
                .build());
        departments.save(Department.builder()
                .id("DPT-002")
                .orgId(ORG_ID)
                .name("Road Maintenance")
                .parentDepartmentId(null)
                .depth(0)
                .officerCount(0)
                .roleDescription("Potholes, resurfacing and footpath upkeep.")
                .build());
    }

    private void seedOfficers() {
        officers.save(Officer.builder()
                .id("OFF-101")
                .orgIds(List.of(ORG_ID))
                .officerUserName("rajesh_kumar")
                .name("Rajesh Kumar")
                .email("rajesh.k@bbmp.gov.in")
                .phone("9988776655")
                .departmentIds(new ArrayList<>(List.of("DPT-001")))
                .isActive(true)
                .createdAt(1791244800000L)
                .build());
        officers.save(Officer.builder()
                .id("OFF-102")
                .orgIds(List.of(ORG_ID))
                .officerUserName("kiran_kumar")
                .name("Kiran Kumar")
                .email("kiran.k@bbmp.gov.in")
                .phone("9988776600")
                .departmentIds(new ArrayList<>(List.of("DPT-002")))
                .isActive(true)
                .managerUserNames(new ArrayList<>(List.of("rajesh_kumar")))
                .createdAt(1791244800000L)
                .build());
    }

    private void seedCitizen() {
        citizens.save(CitizenProfile.builder()
                .username("aisha_patel")
                .name("Aisha Patel")
                .email("aisha.patel@example.com")
                .phone("9876543210")
                .points(1250)
                .tier("Silver Citizen")
                .language("en")
                .build());
        activities.save(PointActivity.builder()
                .citizenUserName("aisha_patel")
                .title("Pothole fixed on 5th Ave")
                .source("Verified by City Council")
                .date("Oct 24")
                .points(100)
                .createdAt(System.currentTimeMillis() - 86_400_000L)
                .build());
    }

    private void seedTasks() {
        long now = System.currentTimeMillis();
        tasks.save(Task.builder()
                .id("PD-8821")
                .groupId("PD-8821")
                .orgId(ORG_ID)
                .citizenUserName("aisha_patel")
                .title("Broken Streetlight")
                .description("Streetlight at 4th Main is flickering.")
                .voiceUrl("")
                .imageUrl("https://url-to-uploaded-image.png")
                .language("English")
                .location(new Task.TaskLocation("Junction of 4th Main and 12th Cross",
                        GeoPoint.of(77.5946, 12.9716)))
                .category("Infrastructure")
                .priority("P2")
                .globalStatus("AI-Assigned")
                .isReviewed(false)
                .reporterType("Citizen")
                .dueDate(now + 4L * 24 * 60 * 60 * 1000)
                .createdAt(now - 2L * 24 * 60 * 60 * 1000)
                .subTasks(new ArrayList<>())
                .comments(new ArrayList<>())
                .notes(new ArrayList<>())
                .activities(new ArrayList<>(List.of(DetailedActivity.builder()
                        .timestamp("Oct 24, 2026, 9:15 AM")
                        .action("AI_ASSIGNED")
                        .performedBy("system_ai")
                        .remarks("Auto-classified as Infrastructure / P2 and routed to Roads & Streetlights")
                        .build())))
                .build());
    }
}
