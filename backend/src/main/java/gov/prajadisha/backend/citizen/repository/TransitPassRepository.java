package gov.prajadisha.backend.citizen.repository;

import gov.prajadisha.backend.citizen.model.TransitPass;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransitPassRepository extends MongoRepository<TransitPass, String> {
    List<TransitPass> findByCitizenUserName(String citizenUserName);
}
