package gov.prajadisha.backend.org.service;

import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    private String uniqueId() {
        String id;
        do {
            id = Ids.prefixed("OFF");
        } while (officers.existsById(id));
        return id;
    }
}
