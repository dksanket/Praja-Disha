package gov.prajadisha.backend.ai.repository;

import gov.prajadisha.backend.ai.model.AiChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends MongoRepository<AiChatMessage, String> {
    List<AiChatMessage> findByOfficerUserNameAndOrgIdOrderByTimestampAsc(String officerUserName, String orgId);
    void deleteByOfficerUserName(String officerUserName);
}
