package gov.prajadisha.backend.org.service;

import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OfficerService {

    private final OfficerRepository officers;

    public OfficerService(OfficerRepository officers) {
        this.officers = officers;
    }

    public List<Officer> list() {
        return officers.findAll();
    }

    public Officer create(Officer input) {
        input.setId(uniqueId());
        if (input.getCreatedAt() == 0) {
            input.setCreatedAt(System.currentTimeMillis());
        }
        if (input.getOrgIds() == null) input.setOrgIds(new ArrayList<>());
        if (input.getDepartmentIds() == null) input.setDepartmentIds(new ArrayList<>());
        if (input.getOfficerUserName() != null
                && officers.findByOfficerUserName(input.getOfficerUserName()).isPresent()) {
            throw ApiException.conflict("Officer username already exists: " + input.getOfficerUserName());
        }
        return officers.save(input);
    }

    public Officer update(String id, Officer input) {
        Officer existing = officers.findById(id)
                .orElseThrow(() -> ApiException.notFound("Officer not found: " + id));
        input.setId(existing.getId());
        if (input.getCreatedAt() == 0) {
            input.setCreatedAt(existing.getCreatedAt());
        }
        return officers.save(input);
    }

    public Optional<Officer> findByIdentifier(String identifier) {
        // Support lookup by username, email, or phone
        return officers.findByOfficerUserName(identifier)
                .or(() -> officers.findByEmail(identifier))
                .or(() -> officers.findByPhone(identifier));
    }

    /**
     * Checks if the officer is active and belongs to at least one organization or department.
     */
    public boolean canLogin(Officer officer) {
        // Comment: Also check if the officer status is active
        if (!officer.isActive()) {
            return false;
        }
        boolean hasOrg = officer.getOrgIds() != null && !officer.getOrgIds().isEmpty();
        boolean hasDept = officer.getDepartmentIds() != null && !officer.getDepartmentIds().isEmpty();
        return hasOrg || hasDept;
    }

    private String uniqueId() {
        String id;
        do {
            id = Ids.prefixed("OFF");
        } while (officers.existsById(id));
        return id;
    }
}
