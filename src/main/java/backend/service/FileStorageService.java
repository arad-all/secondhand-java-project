package backend.service;

import backend.exception.InvalidFileException;
import backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reads and writes advertisement image files on local disk. This is the
 * one place in the app that knows about the filesystem layout — callers
 * (currently just {@link AdvertisementService}) only ever deal with a
 * generated filename, never a real {@link Path}.
 * <p>
 * Layout: {@code {app.upload.dir}/advertisements/{advertisementId}/{uuid}.{ext}}
 * — one subdirectory per advertisement, so images naturally stay grouped
 * and a bulk cleanup (e.g. if an advertisement were ever hard-deleted) is
 * a single directory delete rather than a filename search.
 * <p>
 * Trust boundary: the content type used to pick the stored extension
 * comes from {@link MultipartFile#getContentType()}, which is client-
 * supplied and not independently verified against the file's actual
 * bytes (that would need a magic-byte sniffing library like Apache Tika).
 * This is the same level of validation most small applications rely on;
 * it stops accidental wrong-type uploads, not a determined attacker.
 */
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSIONS_BY_CONTENT_TYPE.keySet();

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Validates and saves one uploaded file for the given advertisement,
     * returning the generated filename (not a full path or URL — the
     * caller decides how that gets exposed). The original client-supplied
     * filename is discarded entirely: the stored name is always a random
     * UUID with an extension derived from the validated content type, so
     * nothing about the caller's input ever becomes part of a path on
     * disk.
     */
    public String store(Long advertisementId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty.");
        }

        String contentType = file.getContentType();
        String extension = (contentType != null) ? EXTENSIONS_BY_CONTENT_TYPE.get(contentType.toLowerCase()) : null;
        if (extension == null) {
            throw new InvalidFileException(
                    "Unsupported image type" + (contentType != null ? " '" + contentType + "'" : "")
                            + ". Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES) + ".");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetDir = advertisementDir(advertisementId);
        Path targetFile = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new InvalidFileException("Failed to store uploaded image: " + e.getMessage());
        }

        return filename;
    }

    /**
     * Loads a previously stored image as a {@link Resource} ready to be
     * streamed back in an HTTP response. {@code filename} is rejected
     * outright (as a 404, same as "doesn't exist") unless it's a bare
     * filename with no path separators — this is the guard against path
     * traversal, since {@code filename} here is untrusted client input
     * (a path variable on the GET endpoint), unlike in {@link #store},
     * where the filename is always server-generated.
     */
    public Resource load(Long advertisementId, String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new ResourceNotFoundException("Image not found.");
        }

        Path file = advertisementDir(advertisementId).resolve(filename).normalize();
        if (!file.startsWith(uploadRoot)) {
            throw new ResourceNotFoundException("Image not found.");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Image not found.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Image not found.");
        }
    }

    /**
     * Best-effort delete, used to clean up a file that was already
     * written to disk when a later step in the same request (e.g. a
     * sibling file in a multi-file upload, or the database write) fails.
     * Deliberately swallows I/O errors: failing the whole request over a
     * cleanup problem would be worse than leaving one orphaned file
     * behind.
     */
    public void delete(Long advertisementId, String filename) {
        try {
            Files.deleteIfExists(advertisementDir(advertisementId).resolve(filename).normalize());
        } catch (IOException ignored) {
            // Orphaned file on disk; not worth failing the request over.
        }
    }

    private Path advertisementDir(Long advertisementId) {
        return uploadRoot.resolve("advertisements").resolve(String.valueOf(advertisementId)).normalize();
    }
}
