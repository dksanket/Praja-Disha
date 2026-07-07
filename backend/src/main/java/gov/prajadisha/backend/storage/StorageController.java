package gov.prajadisha.backend.storage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    public record UploadResponse(String url, String originalName, String contentType, long size) {}
}
