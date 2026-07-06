package gov.prajadisha.backend.citizen.controller;

import gov.prajadisha.backend.citizen.dto.CitizenDtos.CreateTicketRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.CreateTicketResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.FeedbackRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.FeedbackResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.LanguageRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.LanguageResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.LoginRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.LoginResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.RedeemRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.RedeemResponse;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.RegisterRequest;
import gov.prajadisha.backend.citizen.dto.CitizenDtos.ReopenResponse;
import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.service.CitizenService;
import gov.prajadisha.backend.task.model.Task;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Citizen App endpoints (section 2.1 of the API contract).
 *
 * <p>Endpoints that act on behalf of a citizen resolve the current user from the
 * {@code X-Citizen-Username} header (returned by login/register). For local convenience this
 * defaults to the seeded demo citizen when the header is absent.
 */
@RestController
@RequestMapping("/api/citizen")
public class CitizenController {

    private static final String DEFAULT_CITIZEN = "aisha_patel";

    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return citizenService.findByIdentifier(req.identifier())
                .map(profile -> new LoginResponse(true, profile))
                .orElseGet(() -> new LoginResponse(false, null));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<CitizenProfile> register(@Valid @RequestBody RegisterRequest req) {
        CitizenProfile profile = citizenService.register(
                req.name(), req.phone(), req.email(), req.language());
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PostMapping("/tickets")
    public ResponseEntity<CreateTicketResponse> createTicket(
            @RequestHeader(value = "X-Citizen-Username", required = false) String citizen,
            @Valid @RequestBody CreateTicketRequest req) {
        Task task = citizenService.submitTicket(
                resolve(citizen), req.title(), req.description(), req.location(), req.imageUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTicketResponse(task.getId()));
    }

    @PostMapping("/tickets/{id}/feedback")
    public FeedbackResponse feedback(
            @RequestHeader(value = "X-Citizen-Username", required = false) String citizen,
            @PathVariable String id,
            @Valid @RequestBody FeedbackRequest req) {
        return citizenService.submitFeedback(resolve(citizen), id, req.rating(), req.comment());
    }

    @PostMapping("/tickets/{id}/reopen")
    public ReopenResponse reopen(
            @RequestHeader(value = "X-Citizen-Username", required = false) String citizen,
            @PathVariable String id) {
        Task task = citizenService.reopenTicket(resolve(citizen), id);
        return new ReopenResponse(task.getId(), task.getGlobalStatus(), "Ticket successfully reopened.");
    }

    @PutMapping("/profile/language")
    public LanguageResponse updateLanguage(
            @RequestHeader(value = "X-Citizen-Username", required = false) String citizen,
            @Valid @RequestBody LanguageRequest req) {
        String language = citizenService.updateLanguage(resolve(citizen), req.language());
        return new LanguageResponse(true, language);
    }

    @PostMapping("/wallet/redeem")
    public RedeemResponse redeem(
            @RequestHeader(value = "X-Citizen-Username", required = false) String citizen,
            @Valid @RequestBody RedeemRequest req) {
        return citizenService.redeem(resolve(citizen), req.pointsCost(), req.title());
    }

    private String resolve(String citizen) {
        return (citizen == null || citizen.isBlank()) ? DEFAULT_CITIZEN : citizen;
    }
}
