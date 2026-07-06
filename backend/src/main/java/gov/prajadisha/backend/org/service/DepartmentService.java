package gov.prajadisha.backend.org.service;

import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.common.Ids;
import gov.prajadisha.backend.org.model.Department;
import gov.prajadisha.backend.org.model.Officer;
import gov.prajadisha.backend.org.repository.DepartmentRepository;
import gov.prajadisha.backend.org.repository.OfficerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departments;
    private final OfficerRepository officers;

    public DepartmentService(DepartmentRepository departments, OfficerRepository officers) {
        this.departments = departments;
        this.officers = officers;
    }

    public List<Department> list() {
        List<Department> all = departments.findAll();
        all.forEach(this::enrich);
        return all;
    }

    public Department create(Department input) {
        input.setId(uniqueId());
        applyParentAndDepth(input);
        applyHeadOfficer(input);
        input.setOfficerCount((int) officers.findByDepartmentIdsContaining(input.getId()).size());
        return departments.save(input);
    }

    public Department update(String id, Department input) {
        Department existing = departments.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department not found: " + id));
        input.setId(existing.getId());
        applyParentAndDepth(input);
        applyHeadOfficer(input);
        Department saved = departments.save(input);
        enrich(saved);
        return saved;
    }

    private void applyParentAndDepth(Department dept) {
        String parentId = dept.getParentDepartmentId();
        if (parentId == null || parentId.isBlank()) {
            dept.setParentDepartmentId(null);
            dept.setParentDepartmentName(null);
            dept.setDepth(0);
            return;
        }
        departments.findById(parentId).ifPresentOrElse(parent -> {
            dept.setParentDepartmentName(parent.getName());
            dept.setDepth((parent.getDepth() == null ? 0 : parent.getDepth()) + 1);
        }, () -> dept.setDepth(1));
    }

    private void applyHeadOfficer(Department dept) {
        if (dept.getHeadOfficerId() == null || dept.getHeadOfficerId().isBlank()) {
            dept.setHeadOfficerName(null);
            return;
        }
        officers.findById(dept.getHeadOfficerId())
                .ifPresent(o -> dept.setHeadOfficerName(o.getName()));
    }

    private void enrich(Department dept) {
        dept.setOfficerCount((int) officers.findByDepartmentIdsContaining(dept.getId()).size());
        if (dept.getHeadOfficerId() != null && dept.getHeadOfficerName() == null) {
            officers.findById(dept.getHeadOfficerId())
                    .map(Officer::getName)
                    .ifPresent(dept::setHeadOfficerName);
        }
    }

    private String uniqueId() {
        String id;
        do {
            id = Ids.prefixed("DPT");
        } while (departments.existsById(id));
        return id;
    }
}
