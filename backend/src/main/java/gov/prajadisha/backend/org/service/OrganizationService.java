package gov.prajadisha.backend.org.service;

import gov.prajadisha.backend.common.ApiException;
import gov.prajadisha.backend.org.model.Organization;
import gov.prajadisha.backend.org.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

/**
 * Resolves the "active" organization for the current dashboard session.
 *
 * <p>Auth/tenancy is out of scope for the contract, so the active org is simply the first
 * organization on record (the seeded BBMP org in local dev). Replace {@link #getActive()}
 * with a lookup keyed off the authenticated session when auth is introduced.
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizations;

    public OrganizationService(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    public Organization getActive() {
        return organizations.findAll().stream()
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("No active organization configured"));
    }

    public String getActiveOrgId() {
        return getActive().getId();
    }
}
