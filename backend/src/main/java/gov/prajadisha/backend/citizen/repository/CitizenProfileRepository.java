package gov.prajadisha.backend.citizen.repository;

import gov.prajadisha.backend.citizen.model.CitizenProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CitizenProfileRepository extends MongoRepository<CitizenProfile, String> {
    Optional<CitizenProfile> findByUsername(String username);
    Optional<CitizenProfile> findByPhone(String phone);
    Optional<CitizenProfile> findByEmail(String email);
}
