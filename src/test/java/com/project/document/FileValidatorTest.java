package com.project.document;

import com.project.document.service.FileValidator;
import com.project.exception.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
    }

    @Test
    void validate_ValidPdf_ShouldSucceed() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34}; // %PDF-1.4
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", pdfContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_ValidJpeg_ShouldSucceed() {
        byte[] jpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_ValidPng_ShouldSucceed() {
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "id_card.png", "image/png", pngContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_DisguisedExecutableWithPdfExtension_ShouldFailMagicBytes() {
        // Renamed script/binary posing as PDF
        byte[] fakeBytes = "echo 'malicious script'".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "malicious.pdf", "application/pdf", fakeBytes);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_PathTraversalFilename_ShouldFail() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.pdf", "application/pdf", pdfContent);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_UnsupportedExtension_ShouldFail() {
        byte[] content = new byte[]{0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile("file", "program.exe", "application/octet-stream", content);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_EmptyFile_ShouldFail() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_NullFile_ShouldFail() {
        assertThrows(InvalidFileException.class, () -> fileValidator.validate(null));
    }

    @Test
    void validate_OversizedFile_ShouldFail() {
        // 10MB + 1 byte
        byte[] largeBytes = new byte[10 * 1024 * 1024 + 1];
        largeBytes[0] = 0x25; largeBytes[1] = 0x50; largeBytes[2] = 0x44; largeBytes[3] = 0x46; largeBytes[4] = 0x2D;
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_MimeTypeMismatch_ShouldFail() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};
        // Extension says .pdf, but Content-Type claims text/plain
        MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "text/plain", pdfContent);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_NullByteInFilename_ShouldFail() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};
        MockMultipartFile file = new MockMultipartFile("file", "test\0.pdf", "application/pdf", pdfContent);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_CorruptPngMagicBytes_ShouldFail() {
        byte[] badPng = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", badPng);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_CorruptJpegMagicBytes_ShouldFail() {
        byte[] badJpeg = new byte[]{0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", badJpeg);

        assertThrows(InvalidFileException.class, () -> fileValidator.validate(file));
    }
}

