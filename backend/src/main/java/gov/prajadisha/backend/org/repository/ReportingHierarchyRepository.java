package gov.prajadisha.backend.org.repository;

import gov.prajadisha.backend.org.model.ReportingHierarchy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportingHierarchyRepository extends MongoRepository<ReportingHierarchy, String> {
    List<ReportingHierarchy> findByManagerUserName(String managerUserName);
    List<ReportingHierarchy> findByReporteeUserName(String reporteeUserName);
}
