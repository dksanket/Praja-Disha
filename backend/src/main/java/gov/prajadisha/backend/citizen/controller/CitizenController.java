package gov.prajadisha.backend.citizen.controller;

import gov.prajadisha.backend.citizen.dto.CitizenDtos.*;
import gov.prajadisha.backend.citizen.model.CitizenProfile;
import gov.prajadisha.backend.citizen.model.PointActivity;
import gov.prajadisha.backend.citizen.model.TransitPass;
import gov.prajadisha.backend.citizen.service.CitizenService;
import gov.prajadisha.backend.common.JwtService;
import gov.prajadisha.backend.task.model.Task;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Citizen App endpoints (section 2.1 of the API contract).
 *
 * <p>Protected endpoints resolve the current citizen from the JWT token in the
 * {@code Authorization: Bearer <token>} header. If the header is absent (dev/test),
 * the request falls back to {@code X-Citizen-Username} and then to the demo citizen.
 */
@RestController
@RequestMapping("/api/citizen")
public class CitizenController {

    private static final String DEFAULT_CITIZEN = "aisha_patel";

    private final CitizenService citizenService;
    private final JwtService jwtService;

    public CitizenController(CitizenService citizenService, JwtService jwtService) {
        this.citizenService = citizenService;
        this.jwtService = jwtService;
    }

    // -------------------------------------------------------------------- auth

    /**
     * Login: looks up citizen by phone, email, or username.
     * Any provided OTP is accepted for now (real OTP integration is a future phase).
     * Returns a JWT token on success.
     */
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return citizenService.findByIdentifier(req.identifier())
                .map(profile -> {
                    String token = jwtService.generateToken(profile.getUsername());
                    return new LoginResponse(true, profile, token);
                })
                .orElseGet(() -> new LoginResponse(false, null, null));
    }

    /**
     * Register: creates a new citizen profile and returns a JWT token immediately.
     */
    @PostMapping("/auth/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        CitizenProfile profile = citizenService.register(
                req.name(), req.phone(), req.email(), req.language());
        String token = jwtService.generateToken(profile.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LoginResponse(true, profile, token));
    }

    // --------------------------------------------------------------- tickets

    /** Lists all tickets submitted by the authenticated citizen. */
    @GetMapping("/tickets")
    public List<TicketRow> getTickets(HttpServletRequest request) {
        return citizenService.getTickets(resolve(request));
    }

    @PostMapping("/tickets")
    public ResponseEntity<CreateTicketResponse> createTicket(
            HttpServletRequest request,
            @Valid @RequestBody CreateTicketRequest req) {
        Task task = citizenService.submitTicket(
                resolve(request), req.title(), req.description(), req.location(), 
                req.latitude(), req.longitude(), req.imageUrl(), req.voiceUrl(), 
                req.voiceDuration(), req.mediaUrls(), req.language());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateTicketResponse(task.getId()));
    }

    @PostMapping("/tickets/{id}/feedback")
    public FeedbackResponse feedback(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody FeedbackRequest req) {
        return citizenService.submitFeedback(resolve(request), id, req.rating(), req.comment());
    }

    @PostMapping("/tickets/{id}/reopen")
    public ReopenResponse reopen(HttpServletRequest request, @PathVariable String id) {
        Task task = citizenService.reopenTicket(resolve(request), id);
        return new ReopenResponse(task.getId(), task.getGlobalStatus(), "Ticket successfully reopened.");
    }

    // ------------------------------------------------------------- profile

    @PutMapping("/profile/language")
    public LanguageResponse updateLanguage(
            HttpServletRequest request,
            @Valid @RequestBody LanguageRequest req) {
        String language = citizenService.updateLanguage(resolve(request), req.language());
        return new LanguageResponse(true, language);
    }

    // --------------------------------------------------------------- wallet

    @PostMapping("/wallet/redeem")
    public RedeemResponse redeem(
            HttpServletRequest request,
            @Valid @RequestBody RedeemRequest req) {
        return citizenService.redeem(resolve(request), req.pointsCost(), req.title());
    }

    /** Returns all transit passes held by the authenticated citizen. */
    @GetMapping("/wallet/passes")
    public List<TransitPass> getPasses(HttpServletRequest request) {
        return citizenService.getPasses(resolve(request));
    }

    /** Returns the point activity ledger for the authenticated citizen. */
    @GetMapping("/wallet/activities")
    public List<PointActivity> getActivities(HttpServletRequest request) {
        return citizenService.getActivities(resolve(request));
    }

    // ----------------------------------------------------------------- helpers

    /**
     * Resolves the citizen username from (in priority order):
     * 1. JWT token in Authorization: Bearer header
     * 2. X-Citizen-Username header (dev convenience)
     * 3. Hard-coded demo citizen username
     */
    private String resolve(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isValid(token)) {
                return jwtService.extractUsername(token);
            }
        }
        // Fallback for dev/test without a token
        String headerUsername = request.getHeader("X-Citizen-Username");
        if (headerUsername != null && !headerUsername.isBlank()) {
            return headerUsername;
        }
        return DEFAULT_CITIZEN;
    }
}
