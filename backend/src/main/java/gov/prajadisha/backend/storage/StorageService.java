package gov.prajadisha.backend.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import gov.prajadisha.backend.common.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores citizen-uploaded photos, videos and voice recordings.
 * If GCP storage is configured (via gcp.storage.* properties), files are uploaded to the Google Cloud Storage bucket.
 * Otherwise, files are stored on the server's local disk (under {@code app.upload-dir}) and served
 * through the {@code /files/**} resource handler.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    /** Public URL prefix that WebConfig maps back to the upload directory. */
    public static final String PUBLIC_PREFIX = "/files";

    private final String uploadDirConfig;
    private final String publicBaseUrl;
    private final String gcpBucketName;
    private final String gcpProjectId;
    private final String gcpCredentialsPath;

    private Path root;
    private Storage gcpStorage;

    public StorageService(
            @Value("${app.upload-dir:./uploads}") String uploadDir,
            @Value("${app.public-base-url:}") String publicBaseUrl,
            @Value("${gcp.storage.bucket-name:}") String gcpBucketName,
            @Value("${gcp.storage.project-id:}") String gcpProjectId,
            @Value("${gcp.storage.credentials-path:}") String gcpCredentialsPath) {
        this.uploadDirConfig = uploadDir;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        this.gcpBucketName = gcpBucketName;
        this.gcpProjectId = gcpProjectId;
        this.gcpCredentialsPath = gcpCredentialsPath;
    }

    @PostConstruct
    void init() {
        // Always ensure local file storage directories exist as local fallback
        try {
            this.root = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
            Files.createDirectories(root);
            for (String bucket : new String[]{"images", "videos", "audio", "files"}) {
                Files.createDirectories(root.resolve(bucket));
            }
            log.info("Local file storage initialized at {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize local storage directory: " + uploadDirConfig, e);
        }

        // Initialize GCP Storage client if bucket name is configured
        if (gcpBucketName != null && !gcpBucketName.isBlank()) {
            try {
                StorageOptions.Builder builder = StorageOptions.newBuilder();
                if (gcpProjectId != null && !gcpProjectId.isBlank()) {
                    builder.setProjectId(gcpProjectId);
                }
                if (gcpCredentialsPath != null && !gcpCredentialsPath.isBlank()) {
                    java.io.InputStream credentialsStream = getCredentialsStream(gcpCredentialsPath);
                    if (credentialsStream != null) {
                        builder.setCredentials(GoogleCredentials.fromStream(credentialsStream));
                    }
                }
                this.gcpStorage = builder.build().getService();
                log.info("Google Cloud Storage service client initialized for bucket: {}", gcpBucketName);
            } catch (Exception e) {
                log.error("Failed to initialize GCP Storage. Falling back to local storage.", e);
            }
        }
    }

    /** The absolute directory that {@code /files/**} is served from. */
    public Path getRoot() {
        return root;
    }

    /**
     * Persists an uploaded file. If GCP storage client is configured and initialized,
     * uploads directly to Google Cloud Storage. Otherwise, falls back to storing on the local disk.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file provided");
        }
        String bucket = bucketFor(file.getContentType());
        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        // Try GCP upload if bucket name is set
        if (gcpStorage != null && gcpBucketName != null && !gcpBucketName.isBlank()) {
            try {
                BlobId blobId = BlobId.of(gcpBucketName, bucket + "/" + storedName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(file.getContentType())
                        .build();
                // Perform upload to GCP Storage bucket
                gcpStorage.create(blobInfo, file.getBytes());
                // Return a relative proxy URL so the browser fetches through our authenticated backend,
                // rather than hitting the private GCS bucket directly (which would 403).
                String gcpObjectPath = bucket + "/" + storedName;
                log.info("File uploaded successfully to GCP bucket: {}/{}", gcpBucketName, gcpObjectPath);
                return "/api/files/proxy?path=" + java.net.URLEncoder.encode(gcpObjectPath, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("GCP storage upload failed, falling back to local file storage", e);
            }
        }

        // Local Storage Fallback
        Path target = root.resolve(bucket).resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest("Invalid upload path");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store file locally: " + e.getMessage());
        }
        String relativeUrl = PUBLIC_PREFIX + "/" + bucket + "/" + storedName;
        return publicBaseUrl.isEmpty() ? relativeUrl : publicBaseUrl + relativeUrl;
    }

    /**
     * Loads the raw bytes of a file stored either locally or in GCP storage.
     *
     * @param fileUrl the public/relative URL of the stored file
     * @return the file bytes, or null if the file cannot be loaded
     * @throws IOException if local file reading fails
     */
    public byte[] loadFileBytes(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        // Extract the stored filename
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        // Determine bucket name (e.g. images, videos, audio, files)
        String bucket = bucketForFilename(fileName);

        // 1. Try to load from Google Cloud Storage if configured
        if (gcpStorage != null && gcpBucketName != null && !gcpBucketName.isBlank()) {
            // Detect whether fileUrl is a full GCS URL or our new relative proxy path format
            String gcpObjectKey = null;
            if (fileUrl.contains("storage.googleapis.com")) {
                // Legacy full URL — extract bucket path after the bucket name
                String marker = gcpBucketName + "/";
                int idx = fileUrl.indexOf(marker);
                if (idx >= 0) {
                    gcpObjectKey = fileUrl.substring(idx + marker.length());
                }
            } else if (fileUrl.startsWith("/api/files/proxy?path=")) {
                // New relative proxy URL — decode the path parameter
                String encoded = fileUrl.substring("/api/files/proxy?path=".length());
                try {
                    gcpObjectKey = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignore) {}
            }
            if (gcpObjectKey != null) {
                try {
                    com.google.cloud.storage.Blob blob = gcpStorage.get(BlobId.of(gcpBucketName, gcpObjectKey));
                    if (blob != null) {
                        log.info("Loaded file from GCP storage: {}", gcpObjectKey);
                        return blob.getContent();
                    }
                } catch (Exception e) {
                    log.error("Failed to load file from GCP Storage: {}", fileUrl, e);
                }
            }
        }

        // 2. Fall back to local file storage
        Path target = root.resolve(bucket).resolve(fileName).normalize();
        if (Files.exists(target)) {
            log.info("Loaded file from local storage: {}", target);
            return Files.readAllBytes(target);
        }

        log.warn("File not found in local or GCP storage: {}", fileUrl);
        return null;
    }

    private String bucketForFilename(String filename) {
        if (filename == null) return "files";
        String ext = extensionOf(filename).toLowerCase(Locale.ROOT);
        if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")) return "images";
        if (ext.equals(".mp4") || ext.equals(".avi") || ext.equals(".mov") || ext.equals(".mkv")) return "videos";
        if (ext.equals(".wav") || ext.equals(".mp3") || ext.equals(".m4a") || ext.equals(".webm") || ext.equals(".ogg")) return "audio";
        return "files";
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
}
