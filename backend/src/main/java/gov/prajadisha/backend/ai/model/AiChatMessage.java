package gov.prajadisha.backend.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Persisted AI assistant chat message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("ai_chat_messages")
public class AiChatMessage {

    @Id
    private String id;

    @Indexed
    private String officerUserName;

    @Indexed
    private String orgId;

    private String sender; // "ai" or "user"
    private String text;
    private long timestamp; // unix ms
    private List<String> suggestions;
}
