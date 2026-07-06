package gov.prajadisha.backend.task.repository;

import gov.prajadisha.backend.task.model.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByOrgId(String orgId, Pageable pageable);
    List<Task> findByPriority(String priority, Pageable pageable);
    List<Task> findByCitizenUserName(String citizenUserName);
}
