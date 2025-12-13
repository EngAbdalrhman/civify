package com.app.civify.dto.document;

/**
 * Enum representing supported document types for CV processing.
 */
public enum DocumentType {
    PDF("application/pdf", "pdf"),
    PDF_SCANNED("application/pdf", "pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    DOC("application/msword", "doc"),
    IMAGE_PNG("image/png", "png"),
    IMAGE_JPEG("image/jpeg", "jpg"),
    IMAGE_TIFF("image/tiff", "tiff"),
    IMAGE_WEBP("image/webp", "webp"),
    UNKNOWN("application/octet-stream", "bin");

    private final String mimeType;
    private final String extension;

    DocumentType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isPdf() {
        return this == PDF || this == PDF_SCANNED;
    }

    public boolean isImage() {
        return this == IMAGE_PNG || this == IMAGE_JPEG || this == IMAGE_TIFF || this == IMAGE_WEBP;
    }

    public boolean isWord() {
        return this == DOCX || this == DOC;
    }

    public boolean requiresOcr() {
        return this == PDF_SCANNED || isImage();
    }

    public static DocumentType fromMimeType(String mimeType) {
        if (mimeType == null) return UNKNOWN;
        
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> DOCX;
            case "application/msword" -> DOC;
            case "image/png" -> IMAGE_PNG;
            case "image/jpeg", "image/jpg" -> IMAGE_JPEG;
            case "image/tiff" -> IMAGE_TIFF;
            case "image/webp" -> IMAGE_WEBP;
            default -> UNKNOWN;
        };
    }

    public static DocumentType fromExtension(String filename) {
        if (filename == null) return UNKNOWN;
        
        String ext = filename.toLowerCase();
        if (ext.contains(".")) {
            ext = ext.substring(ext.lastIndexOf(".") + 1);
        }
        
        return switch (ext) {
            case "pdf" -> PDF;
            case "docx" -> DOCX;
            case "doc" -> DOC;
            case "png" -> IMAGE_PNG;
            case "jpg", "jpeg" -> IMAGE_JPEG;
            case "tiff", "tif" -> IMAGE_TIFF;
            case "webp" -> IMAGE_WEBP;
            default -> UNKNOWN;
        };
    }
}

