package gov.prajadisha.backend.org.repository;

import gov.prajadisha.backend.org.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrganizationRepository extends MongoRepository<Organization, String> {
}
