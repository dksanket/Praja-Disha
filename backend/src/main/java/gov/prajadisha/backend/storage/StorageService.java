package gov.prajadisha.backend.storage;

import gov.prajadisha.backend.common.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores citizen-uploaded photos, videos and voice recordings on the local server filesystem
 * (under {@code app.upload-dir}) and hands back a public URL that resolves through the
 * {@code /files/**} resource handler (see {@code WebConfig}).
 *
 * <p>Files are bucketed into {@code images/}, {@code videos/}, {@code audio/}, {@code files/}
 * sub-folders by content type and given a random, collision-free name so the original filename
 * (which is attacker-controlled) is never used on disk.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    /** Public URL prefix that WebConfig maps back to the upload directory. */
    public static final String PUBLIC_PREFIX = "/files";

    private final String uploadDirConfig;
    private final String publicBaseUrl;
    private Path root;

    public StorageService(
            @Value("${app.upload-dir:./uploads}") String uploadDir,
            @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.uploadDirConfig = uploadDir;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @PostConstruct
    void init() {
        try {
            this.root = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
            Files.createDirectories(root);
            for (String bucket : new String[]{"images", "videos", "audio", "files"}) {
                Files.createDirectories(root.resolve(bucket));
            }
            log.info("File storage initialized at {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory: " + uploadDirConfig, e);
        }
    }

    /** The absolute directory that {@code /files/**} is served from. */
    public Path getRoot() {
        return root;
    }

    /**
     * Persists an uploaded file and returns its publicly reachable URL
     * (e.g. {@code /files/images/ab12...png} or an absolute URL if {@code app.public-base-url} is set).
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        String bucket = bucketFor(file.getContentType());
        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path target = root.resolve(bucket).resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest("Invalid upload path");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file: " + e.getMessage());
        }
        String relativeUrl = PUBLIC_PREFIX + "/" + bucket + "/" + storedName;
        return publicBaseUrl.isEmpty() ? relativeUrl : publicBaseUrl + relativeUrl;
    }

    private String bucketFor(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/")) return "images";
        if (ct.startsWith("video/")) return "videos";
        if (ct.startsWith("audio/")) return "audio";
        return "files";
    }

    private String extensionOf(String originalFilename) {
        String clean = StringUtils.getFilename(originalFilename);
        if (clean == null) return "";
        String ext = StringUtils.getFilenameExtension(clean);
        if (ext == null || ext.isBlank()) return "";
        // keep it short and safe
        String safe = ext.replaceAll("[^A-Za-z0-9]", "");
        return safe.isEmpty() ? "" : "." + safe.toLowerCase(Locale.ROOT);
    }
}
