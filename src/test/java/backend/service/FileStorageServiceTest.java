package backend.service;

import backend.exception.InvalidFileException;
import backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    // ---- store ----

    @Test
    void store_acceptsJpegAndReturnsJpgExtension() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        doNothing().when(file).transferTo(any(Path.class));

        String filename = fileStorageService.store(1L, file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".jpg"));
    }

    @Test
    void store_acceptsPngAndReturnsPngExtension() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        doNothing().when(file).transferTo(any(Path.class));

        String filename = fileStorageService.store(1L, file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".png"));
    }

    @Test
    void store_rejectsUnsupportedContentType() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThrows(InvalidFileException.class,
                () -> fileStorageService.store(1L, file));
    }

    @Test
    void store_rejectsEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(InvalidFileException.class,
                () -> fileStorageService.store(1L, file));
    }

    @Test
    void store_rejectsNullFile() {
        assertThrows(InvalidFileException.class,
                () -> fileStorageService.store(1L, null));
    }

    @Test
    void store_createsDirectoryAndWritesFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        doAnswer(inv -> {
            Path path = inv.getArgument(0);
            Files.createDirectories(path.getParent());
            Files.writeString(path, "image data");
            return null;
        }).when(file).transferTo(any(Path.class));

        Path targetDir = tempDir.resolve("advertisements/42");
        String filename = fileStorageService.store(42L, file);

        assertTrue(Files.exists(targetDir));
        assertTrue(Files.exists(targetDir.resolve(filename)));
    }

    @Test
    void store_generatesUniqueFilenames() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg");
        doNothing().when(file).transferTo(any(Path.class));

        String name1 = fileStorageService.store(1L, file);
        String name2 = fileStorageService.store(1L, file);

        assertNotEquals(name1, name2);
    }

    // ---- load ----

    @Test
    void load_returnsExistingFile() throws IOException {
        Path adDir = tempDir.resolve("advertisements/1");
        Files.createDirectories(adDir);
        Files.writeString(adDir.resolve("photo.jpg"), "test image content");

        Resource resource = fileStorageService.load(1L, "photo.jpg");

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void load_throws404_whenFileDoesNotExist() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, "nonexistent.jpg"));
    }

    @Test
    void load_rejectsPathTraversalWithDotDot() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, "../secret.txt"));
    }

    @Test
    void load_rejectsPathTraversalWithForwardSlash() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, "subdir/../../etc/passwd"));
    }

    @Test
    void load_rejectsPathTraversalWithBackslash() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, "..\\windows\\system32\\config"));
    }

    @Test
    void load_rejectsNullFilename() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, null));
    }

    @Test
    void load_rejectsBlankFilename() {
        assertThrows(ResourceNotFoundException.class,
                () -> fileStorageService.load(1L, "   "));
    }

    // ---- delete ----

    @Test
    void delete_removesExistingFile() throws IOException {
        Path adDir = tempDir.resolve("advertisements/1");
        Files.createDirectories(adDir);
        Files.writeString(adDir.resolve("delete-me.jpg"), "to be deleted");

        fileStorageService.delete(1L, "delete-me.jpg");

        assertFalse(Files.exists(adDir.resolve("delete-me.jpg")));
    }

    @Test
    void delete_doesNotThrow_whenFileDoesNotExist() {
        assertDoesNotThrow(() -> fileStorageService.delete(1L, "already-gone.jpg"));
    }
}
