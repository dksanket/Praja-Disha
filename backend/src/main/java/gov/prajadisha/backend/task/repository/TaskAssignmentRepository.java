package gov.prajadisha.backend.task.repository;

import gov.prajadisha.backend.task.model.TaskAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TaskAssignmentRepository extends MongoRepository<TaskAssignment, String> {
    List<TaskAssignment> findByTaskId(String taskId);
    void deleteByTaskId(String taskId);
}
