package gov.prajadisha.backend.org.repository;

import gov.prajadisha.backend.org.model.OrganizationConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrganizationConfigRepository extends MongoRepository<OrganizationConfig, String> {
    Optional<OrganizationConfig> findByOrgId(String orgId);
}
