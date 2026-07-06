package gov.prajadisha.backend.org.repository;

import gov.prajadisha.backend.org.model.Officer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OfficerRepository extends MongoRepository<Officer, String> {
    Optional<Officer> findByOfficerUserName(String officerUserName);
    List<Officer> findByDepartmentIdsContaining(String departmentId);
}
