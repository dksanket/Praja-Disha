package gov.prajadisha.backend.citizen.repository;

import gov.prajadisha.backend.citizen.model.PointActivity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PointActivityRepository extends MongoRepository<PointActivity, String> {
    List<PointActivity> findByCitizenUserNameOrderByCreatedAtDesc(String citizenUserName);
}
