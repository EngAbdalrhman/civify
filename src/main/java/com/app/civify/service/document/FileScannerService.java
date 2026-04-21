package com.app.civify.service.document;

import com.app.civify.dto.document.FileScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Heuristic file security scanner that inspects uploaded document content
 * for common malware/threat indicators before any processing or storage.
 *
 * <p>Detected threats:</p>
 * <ul>
 *   <li>JavaScript / launch actions embedded in PDFs</li>
 *   <li>Encrypted/password-protected PDFs (cannot be parsed, common obfuscation)</li>
 *   <li>VBA macros and OLE objects inside Office (DOCX/XLSX) files</li>
 *   <li>Polyglot / disguised executables (PE, ELF, shell scripts)</li>
 *   <li>EICAR antivirus test signature</li>
 * </ul>
 *
 * <p>This is <b>not</b> a full antivirus engine. It is a defense-in-depth layer
 * that catches the most common weaponized document attack vectors. For
 * enterprise-grade scanning, a ClamAV integration can be added behind this
 * same service interface.</p>
 */
@Service
@Slf4j
public class FileScannerService {

    @Value("${civify.security.file-scan.enabled:true}")
    private boolean scanEnabled;

    /** EICAR standard antivirus test string. */
    private static final String EICAR_SIGNATURE =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    // ── PDF threat markers ───────────────────────────────────────────────
    // Only markers that are inherently dangerous on their own.
    // /OpenAction and /AA are STANDARD PDF features (page view, zoom, annotations)
    // and are present in virtually every PDF. They are only dangerous when they
    // reference JavaScript, which we detect via the JS markers below.
    private static final String[] PDF_JS_MARKERS = {
            "/JavaScript", "/JS "
    };

    // Additional JS patterns that appear in PDF object streams
    private static final String[] PDF_JS_PATTERNS = {
            "/JS(", "/JS<", "/JS/"
    };

    private static final String PDF_ENCRYPT_MARKER = "/Encrypt";

    // ── Office ZIP-entry threat markers ──────────────────────────────────
    private static final String[] OFFICE_THREAT_ENTRIES = {
            "vbaproject.bin",
            "oleobject"
    };

    // ── Executable magic bytes ───────────────────────────────────────────
    /** DOS/PE executable: MZ header */
    private static final byte[] MZ_HEADER = {(byte) 0x4D, (byte) 0x5A};
    /** Linux ELF executable: 0x7F E L F */
    private static final byte[] ELF_HEADER = {(byte) 0x7F, 0x45, 0x4C, 0x46};
    /** Shell script shebang: #! */
    private static final byte[] SHEBANG_HEADER = {(byte) 0x23, (byte) 0x21};

    /**
     * Scan a {@link MultipartFile} for known threat indicators.
     *
     * @param file the uploaded file to inspect
     * @return {@link FileScanResult} — clean or threat with details
     */
    public FileScanResult scanFile(MultipartFile file) {
        if (!scanEnabled) {
            log.debug("File security scanning is disabled, skipping.");
            return FileScanResult.safe();
        }

        if (file == null || file.isEmpty()) {
            return FileScanResult.safe(); // Emptiness is validated elsewhere
        }

        try {
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().toLowerCase() : "";

            // 1. EICAR test signature
            FileScanResult eicarResult = checkEicar(fileBytes);
            if (!eicarResult.isClean()) return eicarResult;

            // 2. Executable disguise check (regardless of extension/MIME)
            FileScanResult execResult = checkExecutableMagic(fileBytes);
            if (!execResult.isClean()) return execResult;

            // 3. PDF-specific scans
            if (isPdf(fileBytes, fileName)) {
                FileScanResult pdfResult = scanPdf(fileBytes);
                if (!pdfResult.isClean()) return pdfResult;
            }

            // 4. Office ZIP-based scans (DOCX, XLSX, PPTX)
            if (isZipBased(fileBytes)) {
                FileScanResult officeResult = scanOfficeZip(fileBytes);
                if (!officeResult.isClean()) return officeResult;
            }

            log.debug("File '{}' passed security scan.", file.getOriginalFilename());
            return FileScanResult.safe();

        } catch (Exception e) {
            log.error("Error during file security scan for '{}': {}",
                    file.getOriginalFilename(), e.getMessage());
            // Fail-open: if scanning crashes, let the file through and log the error.
            // Change to fail-closed if preferred:
            // return FileScanResult.threat("SCAN_ERROR", "Security scan failed: " + e.getMessage());
            return FileScanResult.safe();
        }
    }

    /**
     * Scan raw byte content (used by {@code processDocument(byte[], ...)}).
     */
    public FileScanResult scanBytes(byte[] data, String mimeType) {
        if (!scanEnabled || data == null || data.length == 0) {
            return FileScanResult.safe();
        }

        try {
            // 1. EICAR
            FileScanResult eicarResult = checkEicar(data);
            if (!eicarResult.isClean()) return eicarResult;

            // 2. Executable disguise
            FileScanResult execResult = checkExecutableMagic(data);
            if (!execResult.isClean()) return execResult;

            // 3. PDF
            if (isPdf(data, mimeType != null ? mimeType : "")) {
                FileScanResult pdfResult = scanPdf(data);
                if (!pdfResult.isClean()) return pdfResult;
            }

            // 4. Office ZIP
            if (isZipBased(data)) {
                FileScanResult officeResult = scanOfficeZip(data);
                if (!officeResult.isClean()) return officeResult;
            }

            return FileScanResult.safe();
        } catch (Exception e) {
            log.error("Error during byte-array security scan: {}", e.getMessage());
            return FileScanResult.safe();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private FileScanResult checkEicar(byte[] data) {
        String content = new String(data, StandardCharsets.ISO_8859_1);
        if (content.contains(EICAR_SIGNATURE)) {
            log.warn("EICAR test signature detected in uploaded file.");
            return FileScanResult.threat("EICAR_TEST_SIGNATURE",
                    "File contains the EICAR antivirus test signature.");
        }
        return FileScanResult.safe();
    }

    private FileScanResult checkExecutableMagic(byte[] data) {
        if (data.length < 4) return FileScanResult.safe();

        if (startsWith(data, MZ_HEADER)) {
            log.warn("Executable (PE/MZ) magic bytes detected in uploaded file.");
            return FileScanResult.threat("EXECUTABLE_PE",
                    "File appears to be a Windows executable disguised as a document.");
        }
        if (startsWith(data, ELF_HEADER)) {
            log.warn("Executable (ELF) magic bytes detected in uploaded file.");
            return FileScanResult.threat("EXECUTABLE_ELF",
                    "File appears to be a Linux executable disguised as a document.");
        }
        if (startsWith(data, SHEBANG_HEADER)) {
            log.warn("Shell script shebang detected in uploaded file.");
            return FileScanResult.threat("EXECUTABLE_SCRIPT",
                    "File appears to be a script disguised as a document.");
        }
        return FileScanResult.safe();
    }

    /**
     * Scan PDF content for embedded JavaScript and encryption.
     *
     * <p><b>Important:</b> We intentionally do NOT flag {@code /OpenAction} or
     * {@code /AA} on their own — they are standard PDF operators present in
     * virtually every PDF (for initial page view, zoom, annotations). They are
     * only dangerous when they reference JavaScript, which we detect via the
     * {@code /JavaScript} and {@code /JS} markers.</p>
     *
     * <p>Similarly, {@code /Launch} is only flagged when it co-occurs with
     * action-triggering operators, indicating an attempt to execute an external
     * program.</p>
     */
    private FileScanResult scanPdf(byte[] data) {
        // Convert relevant portions to string for marker searching
        int headSize = Math.min(data.length, 100 * 1024);
        String headContent = new String(data, 0, headSize, StandardCharsets.ISO_8859_1);

        // Also check the tail (cross-ref / trailer area)
        int tailStart = Math.max(0, data.length - 20 * 1024);
        String tailContent = tailStart > 0
                ? new String(data, tailStart, data.length - tailStart, StandardCharsets.ISO_8859_1)
                : "";

        String combined = headContent + tailContent;

        // 1. Check for explicit JavaScript markers (always dangerous)
        for (String marker : PDF_JS_MARKERS) {
            if (combined.contains(marker)) {
                log.warn("PDF JavaScript marker '{}' detected in uploaded file.", marker);
                return FileScanResult.threat("JAVASCRIPT_IN_PDF",
                        "PDF contains embedded JavaScript (" + marker.trim() + ").");
            }
        }

        // 2. Check for additional JS patterns in object streams
        for (String pattern : PDF_JS_PATTERNS) {
            if (combined.contains(pattern)) {
                log.warn("PDF JavaScript pattern '{}' detected in uploaded file.", pattern);
                return FileScanResult.threat("JAVASCRIPT_IN_PDF",
                        "PDF contains embedded JavaScript.");
            }
        }

        // 3. Check for /Launch action (attempt to execute external program)
        //    Only flag when combined with /Action or /S (action type specifier)
        if (combined.contains("/Launch") &&
                (combined.contains("/S /Launch") || combined.contains("/S/Launch"))) {
            log.warn("PDF Launch action detected in uploaded file.");
            return FileScanResult.threat("LAUNCH_ACTION_IN_PDF",
                    "PDF contains a Launch action that attempts to execute an external program.");
        }

        // 4. Check for encryption
        if (combined.contains(PDF_ENCRYPT_MARKER)) {
            log.warn("Encrypted/password-protected PDF detected.");
            return FileScanResult.threat("ENCRYPTED_PDF",
                    "Password-protected or encrypted PDFs cannot be processed.");
        }

        return FileScanResult.safe();
    }

    /**
     * Scan a ZIP-based Office document for VBA macros and OLE objects.
     */
    private FileScanResult scanOfficeZip(byte[] data) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();

                for (String threat : OFFICE_THREAT_ENTRIES) {
                    if (entryName.contains(threat)) {
                        String threatType = entryName.contains("vbaproject")
                                ? "VBA_MACRO" : "OLE_OBJECT";
                        log.warn("Office threat '{}' detected in ZIP entry '{}'.", threatType, entry.getName());
                        return FileScanResult.threat(threatType,
                                "Document contains embedded " +
                                        (threatType.equals("VBA_MACRO") ? "VBA macros" : "OLE objects") +
                                        " which may be harmful.");
                    }
                }

                zis.closeEntry();
            }
        } catch (Exception e) {
            // Not a valid ZIP or I/O error — skip silently (type detection will handle it)
            log.debug("Could not scan ZIP entries: {}", e.getMessage());
        }
        return FileScanResult.safe();
    }

    // ── Detection helpers ────────────────────────────────────────────────

    private boolean isPdf(byte[] data, String nameOrMime) {
        if (startsWith(data, "%PDF-".getBytes(StandardCharsets.US_ASCII))) return true;
        return nameOrMime.contains("pdf");
    }

    private boolean isZipBased(byte[] data) {
        return data.length >= 4
                && data[0] == (byte) 0x50
                && data[1] == (byte) 0x4B
                && data[2] == (byte) 0x03
                && data[3] == (byte) 0x04;
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data == null || prefix == null || data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
