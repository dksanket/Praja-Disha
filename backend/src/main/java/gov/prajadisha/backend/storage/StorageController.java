package gov.prajadisha.backend.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * File upload endpoint. Citizens (and officers) upload a photo, video or voice recording here,
 * receive a local URL, and then pass that URL as {@code imageUrl} / {@code voiceUrl} when
 * submitting a ticket. Files are stored on the server's local disk (see {@link StorageService}).
 */
@RestController
@RequestMapping("/api/files")
public class StorageController {

    private final StorageService storage;

    public StorageController(StorageService storage) {
        this.storage = storage;
    }

    /** Uploads a single file. Returns {@code {"url": "/files/images/...."}}. */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String url = storage.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadResponse(url, file.getOriginalFilename(),
                        file.getContentType(), file.getSize()));
    }

    /** Uploads several files at once. Returns a list of results in the same order. */
    @PostMapping("/upload-multiple")
    public ResponseEntity<List<UploadResponse>> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files) {
        List<UploadResponse> results = files.stream()
                .map(f -> new UploadResponse(storage.store(f), f.getOriginalFilename(),
                        f.getContentType(), f.getSize()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * Proxies a private GCP-stored media file through the backend so browsers can
     * play audio and render images/videos without needing direct GCS access.
     * The {@code path} parameter is the GCS object path (e.g. {@code audio/filename.wav}).
     */
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String path) {
        // Reconstruct the relative proxy URL so loadFileBytes can resolve it
        String proxyUrl = "/api/files/proxy?path=" +
                java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8);
        try {
            byte[] bytes = storage.loadFileBytes(proxyUrl);
            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            // Detect content-type from file extension for correct browser handling
            MediaType mediaType = guessMediaType(path);
            return ResponseEntity.ok().contentType(mediaType).body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Best-effort media type detection based on file extension. */
    private MediaType guessMediaType(String path) {
        if (path == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = path.toLowerCase();
        if (lower.endsWith(".wav")) return MediaType.valueOf("audio/wav");
        if (lower.endsWith(".mp3")) return MediaType.valueOf("audio/mpeg");
        if (lower.endsWith(".webm")) return MediaType.valueOf("audio/webm");
        if (lower.endsWith(".ogg")) return MediaType.valueOf("audio/ogg");
        if (lower.endsWith(".m4a")) return MediaType.valueOf("audio/mp4");
        if (lower.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public record UploadResponse(String url, String originalName, String contentType, long size) {}
}
