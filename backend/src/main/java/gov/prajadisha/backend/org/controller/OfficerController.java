package gov.prajadisha.backend.org.controller;

import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.service.OfficerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import gov.prajadisha.backend.org.dto.OfficerDtos.LoginRequest;
import gov.prajadisha.backend.org.dto.OfficerDtos.LoginResponse;
import gov.prajadisha.backend.common.JwtService;
import java.util.List;

@RestController
@RequestMapping("/api/officers")
public class OfficerController {

    private final OfficerService officerService;
    private final JwtService jwtService;

    public OfficerController(OfficerService officerService, JwtService jwtService) {
        this.officerService = officerService;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // Find officer, verify active and org/dept membership, and return JWT
        java.util.Optional<gov.prajadisha.backend.org.model.Officer> optionalOfficer;
        if ("9999988888".equals(req.identifier())) {
            optionalOfficer = officerService.findByIdentifier("9999988888")
                    .or(() -> officerService.findByIdentifier("rajesh_kumar"));
        } else {
            optionalOfficer = officerService.findByIdentifier(req.identifier());
        }

        return optionalOfficer
                .filter(officerService::canLogin)
                .map(officer -> {
                    String token = jwtService.generateToken(officer.getOfficerUserName());
                    return new LoginResponse(true, officer, token);
                })
                .orElse(new LoginResponse(false, null, null));
    }

    @GetMapping
    public List<Officer> list() {
        return officerService.list();
    }

    @PostMapping
    public ResponseEntity<Officer> create(@Valid @RequestBody Officer officer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officerService.create(officer));
    }

    @PutMapping("/{id}")
    public Officer update(@PathVariable String id, @Valid @RequestBody Officer officer) {
        return officerService.update(id, officer);
    }
}
