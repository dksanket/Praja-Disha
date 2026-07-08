package gov.prajadisha.backend.ai.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.util.*;

/**
 * Service to transcribe voice messages using the Google Cloud Speech-to-Text API.
 * Uses the service account credentials file to obtain OAuth2 access tokens and makes
 * direct REST calls to the Google Speech API.
 */
@Service
public class GoogleSpeechService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSpeechService.class);

    private final String credentialsPath;
    private final RestTemplate restTemplate;

    public GoogleSpeechService(@Value("${gcp.storage.credentials-path:}") String credentialsPath) {
        this.credentialsPath = credentialsPath;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Transcribes the given audio bytes into text.
     *
     * @param audioBytes   the raw audio file bytes (e.g. WAV or WEBM container)
     * @param languageCode the BCP-47 speech language code (e.g. "kn-IN", "hi-IN", "en-IN")
     * @return the transcribed text, or empty string on failure
     */
    @SuppressWarnings("unchecked")
    public String transcribe(byte[] audioBytes, String languageCode) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("GCP credentials path is not set; skipping Speech-to-Text transcription.");
            return "";
        }

        try {
            // 1. Obtain Google OAuth2 access token
            java.io.InputStream credentialsStream = getCredentialsStream(credentialsPath);
            if (credentialsStream == null) {
                log.warn("GCP credentials are not set; skipping Speech-to-Text transcription.");
                return "";
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            // 2. Base64-encode the audio bytes
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // 3. Prepare the request payload structure
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("languageCode", languageCode != null && !languageCode.isBlank() ? languageCode : "en-IN");
            config.put("alternativeLanguageCodes", Arrays.asList("kn-IN", "hi-IN", "en-IN"));
            // Use latest_long for higher accuracy with spontaneous/conversational speech
            config.put("model", "latest_long");
            // Punctuation helps the downstream translation model produce cleaner output
            config.put("enableAutomaticPunctuation", true);

            Map<String, Object> audio = new LinkedHashMap<>();
            audio.put("content", base64Audio);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("config", config);
            body.put("audio", audio);

            // 4. Set Headers with Authorization
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 5. Invoke Google Speech-to-Text recognize API endpoint
            String url = "https://speech.googleapis.com/v1/speech:recognize";
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                StringBuilder transcriptBuilder = new StringBuilder();
                for (Map<String, Object> result : results) {
                    List<Map<String, Object>> alternatives = (List<Map<String, Object>>) result.get("alternatives");
                    if (alternatives != null && !alternatives.isEmpty()) {
                        String piece = (String) alternatives.get(0).get("transcript");
                        if (piece != null) {
                            transcriptBuilder.append(piece).append(" ");
                        }
                    }
                }
                return transcriptBuilder.toString().trim();
            }
        } catch (Exception e) {
            log.error("Google Speech-to-Text transcription encountered an error: {}", e.getMessage(), e);
        }

        return "";
    }

    private java.io.InputStream getCredentialsStream(String config) throws java.io.IOException {
        if (config == null || config.isBlank()) {
            return null;
        }
        String trimmed = config.trim();
        if (trimmed.startsWith("{")) {
            return new java.io.ByteArrayInputStream(trimmed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        String resolvedPath = resolveCredentialsPath(trimmed);
        return new java.io.FileInputStream(resolvedPath);
    }

    private String resolveCredentialsPath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        java.io.File file = new java.io.File(path);
        if (file.isDirectory()) {
            java.io.File[] files = file.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                log.info("Resolved credentials directory '{}' to file '{}'", path, files[0].getAbsolutePath());
                return files[0].getAbsolutePath();
            }
        }
        return path;
    }
}
