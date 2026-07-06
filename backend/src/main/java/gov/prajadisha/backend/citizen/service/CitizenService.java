package gov.prajadisha.backend.citizen.service;

import gov.prajadisha.backend.citizen.dto.CitizenDtos.FeedbackResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.RedeemResponse;
import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.PointActivity;
import gov.prajadisha.backend.citizen.model.TransitPass;
import gov.prajadisha.backend.citizen.repository.CitizenProfileRepository;
import gov.prajadisha.backend.citizen.repository.PointActivityRepository;
import gov.prajadisha.backend.citizen.repository.TransitPassRepository;
import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Formats;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.task.model.Task;
import gov.prajadisha.backend.task.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class CitizenService {

    private static final int FEEDBACK_AWARD = 50;
    private static final int REGISTRATION_BONUS = 200;

    private final CitizenProfileRepository citizens;
    private final TransitPassRepository passes;
    private final PointActivityRepository activities;
    private final TierService tierService;
    private final TaskService taskService;

    public CitizenService(CitizenProfileRepository citizens, TransitPassRepository passes,
                          PointActivityRepository activities, TierService tierService,
                          TaskService taskService) {
        this.citizens = citizens;
        this.passes = passes;
        this.activities = activities;
        this.tierService = tierService;
        this.taskService = taskService;
    }

    // -------------------------------------------------------------------- auth

    public Optional<CitizenProfile> findByIdentifier(String identifier) {
        return citizens.findByPhone(identifier)
                .or(() -> citizens.findByEmail(identifier))
                .or(() -> citizens.findByUsername(identifier));
    }

    public CitizenProfile register(String name, String phone, String email, String language) {
        String username = uniqueUsername(name);
        String resolvedEmail = (email == null || email.isBlank())
                ? username.replace("_", ".") + "@example.com"
                : email;
        CitizenProfile profile = CitizenProfile.builder()
                .username(username)
                .name(name)
                .email(resolvedEmail)
                .phone(phone)
                .points(REGISTRATION_BONUS)
                .tier(tierService.tierFor(REGISTRATION_BONUS))
                .language((language == null || language.isBlank()) ? "en" : language)
                .build();
        CitizenProfile saved = citizens.save(profile);
        activities.save(PointActivity.builder()
                .citizenUserName(saved.getUsername())
                .title("Welcome bonus")
                .source("Registration reward")
                .date(Formats.shortOrToday(System.currentTimeMillis()))
                .points(REGISTRATION_BONUS)
                .createdAt(System.currentTimeMillis())
                .build());
        return saved;
    }

    public CitizenProfile requireCitizen(String username) {
        return citizens.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Citizen not found: " + username));
    }

    // ---------------------------------------------------------------- language

    public String updateLanguage(String username, String language) {
        CitizenProfile profile = requireCitizen(username);
        profile.setLanguage(language);
        citizens.save(profile);
        return language;
    }

    // ---------------------------------------------------------------- tickets

    public Task submitTicket(String username, String title, String description,
                             String location, String imageUrl) {
        // ensure the citizen exists (keeps the ledger consistent)
        requireCitizen(username);
        return taskService.createTicket(username, title, description, location, imageUrl);
    }

    public Task reopenTicket(String username, String ticketId) {
        return taskService.reopen(ticketId);
    }

    public FeedbackResponse submitFeedback(String username, String ticketId, int rating, String comment) {
        CitizenProfile profile = requireCitizen(username);
        taskService.recordFeedback(ticketId, rating, comment);

        int updated = profile.getPoints() + FEEDBACK_AWARD;
        profile.setPoints(updated);
        profile.setTier(tierService.tierFor(updated));
        citizens.save(profile);

        activities.save(PointActivity.builder()
                .citizenUserName(username)
                .title("Feedback reward for " + ticketId)
                .source("Verified resolution feedback")
                .date(Formats.shortOrToday(System.currentTimeMillis()))
                .points(FEEDBACK_AWARD)
                .createdAt(System.currentTimeMillis())
                .build());

        return new FeedbackResponse(
                "Feedback submitted. Points awarded successfully.",
                FEEDBACK_AWARD, updated, profile.getTier());
    }

    // ------------------------------------------------------------------ wallet

    public RedeemResponse redeem(String username, int pointsCost, String title) {
        CitizenProfile profile = requireCitizen(username);
        if (profile.getPoints() < pointsCost) {
            throw ApiException.badRequest("Insufficient points to redeem this pass");
        }
        int updated = profile.getPoints() - pointsCost;
        profile.setPoints(updated);
        profile.setTier(tierService.tierFor(updated));
        citizens.save(profile);

        String passId = Ids.prefixed("pass-active");
        String expiresAt = LocalTime.now(Formats.ZONE).plusHours(1)
                .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)) + " PM";
        TransitPass pass = TransitPass.builder()
                .citizenUserName(username)
                .title(title)
                .pointsCost(pointsCost)
                .expiresAt(expiresAt)
                .fareType("Single Trip")
                .qrCodeData("https://praja-disha.gov.in/pass/verify/" + passId)
                .isActive(true)
                .build();
        TransitPass savedPass = passes.save(pass);

        activities.save(PointActivity.builder()
                .citizenUserName(username)
                .title("Redeemed " + title)
                .source("Transit rewards store")
                .date(Formats.shortOrToday(System.currentTimeMillis()))
                .points(-pointsCost)
                .createdAt(System.currentTimeMillis())
                .build());

        return new RedeemResponse(true, updated, savedPass);
    }

    // ----------------------------------------------------------------- helpers

    private String uniqueUsername(String name) {
        String base = Ids.usernameFromName(name);
        String candidate = base;
        int suffix = 1;
        while (citizens.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + (++suffix);
        }
        return candidate;
    }
}
