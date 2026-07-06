package gov.prajadisha.backend.org.repository;

import gov.prajadisha.backend.org.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    List<Department> findByOrgId(String orgId);
    List<Department> findByParentDepartmentId(String parentDepartmentId);
    long countByParentDepartmentId(String parentDepartmentId);
}
