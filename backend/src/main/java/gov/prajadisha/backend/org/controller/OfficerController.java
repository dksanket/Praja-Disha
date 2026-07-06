package gov.prajadisha.backend.org.controller;

import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.service.OfficerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officers")
public class OfficerController {

    private final OfficerService officerService;

    public OfficerController(OfficerService officerService) {
        this.officerService = officerService;
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
