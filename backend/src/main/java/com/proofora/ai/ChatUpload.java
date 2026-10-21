package com.proofora.ai;

import com.proofora.exception.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ChatUpload {
    public static final int MAX_BYTES = 4 * 1024 * 1024;
    public static final int MAX_FILES = 3;
    public static final int MAX_TEXT = 12000;

    private final String filename;
    private final String mediaType;
    private final byte[] data;
    private final boolean image;
    private final String text;

    private ChatUpload(String filename, String mediaType, byte[] data, boolean image, String text) {
        this.filename = filename;
        this.mediaType = mediaType;
        this.data = data;
        this.image = image;
        this.text = text;
    }

    public String filename() { return filename; }
    public String mediaType() { return mediaType; }
    public byte[] data() { return data; }
    public boolean image() { return image; }
    public String text() { return text; }

    public static ChatUpload parse(String originalName, byte[] data) {
        if (data == null || data.length == 0) {
            throw ApiException.badRequest("The attachment is empty.");
        }
        if (data.length > MAX_BYTES) {
            throw ApiException.badRequest("Each attachment must be 4 MB or smaller.");
        }
        String name = safeName(originalName);
        String ext = extension(name);
        return switch (ext) {
            case "pdf" -> document(name, "application/pdf", data, pdfText(data));
            case "docx" -> document(name, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", data, docxText(data));
            case "txt" -> document(name, "text/plain", data, plainText(data));
            case "jpg", "jpeg" -> image(name, "image/jpeg", data, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png" -> image(name, "image/png", data, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
            case "webp" -> webp(name, data);
            default -> throw ApiException.badRequest("Attach a PDF, Word (.docx), text, or image file (JPG, PNG, WEBP).");
        };
    }

    private static ChatUpload document(String name, String mediaType, byte[] data, String text) {
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest(name + " did not contain readable text.");
        }
        return new ChatUpload(name, mediaType, data, false, trim(text));
    }

    private static ChatUpload image(String name, String mediaType, byte[] data, byte[] magic) {
        if (!startsWith(data, magic)) {
            throw ApiException.badRequest(name + " is not a valid " + mediaType + " file.");
        }
        return new ChatUpload(name, mediaType, data, true, null);
    }

    private static ChatUpload webp(String name, byte[] data) {
        if (data.length < 12 || !startsWith(data, new byte[]{'R', 'I', 'F', 'F'})
                || data[8] != 'W' || data[9] != 'E' || data[10] != 'B' || data[11] != 'P') {
            throw ApiException.badRequest(name + " is not a valid WEBP image.");
        }
        return new ChatUpload(name, "image/webp", data, true, null);
    }

    static String pdfText(byte[] data) {
        if (!startsWith(data, "%PDF".getBytes(StandardCharsets.US_ASCII))) {
            throw ApiException.badRequest("That PDF is not a valid PDF file.");
        }
        try (PDDocument document = Loader.loadPDF(data)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not read that PDF.");
        }
    }

    static String docxText(byte[] data) {
        if (!startsWith(data, new byte[]{'P', 'K'})) {
            throw ApiException.badRequest("That Word file is not a valid .docx document.");
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    String text = xml.replaceAll("</w:p>", "\n")
                            .replaceAll("<[^>]+>", "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace("&apos;", "'");
                    return text;
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not read that Word document.");
        }
        throw ApiException.badRequest("That Word document has no readable text.");
    }

    private static String plainText(byte[] data) {
        for (byte b : data) {
            if (b == 0) {
                throw ApiException.badRequest("Text attachments must be plain UTF-8 text.");
            }
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private static String safeName(String originalName) {
        String name = originalName == null ? "attachment" : originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        name = slash >= 0 ? name.substring(slash + 1) : name;
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            return "attachment";
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static String trim(String text) {
        String cleaned = text.replace("\u0000", "").trim();
        return cleaned.length() <= MAX_TEXT ? cleaned : cleaned.substring(0, MAX_TEXT);
    }
}
