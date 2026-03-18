package com.contractGuard.LegalLens.service.parser;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class DocumentParserService {

    public String parseDocument(MultipartFile file) throws IOException {
        log.info("Parsing document: {}, Type: {}", file.getOriginalFilename(), file.getContentType());

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType();

        if (filename.endsWith(".pdf") || (contentType != null && contentType.contains("pdf"))) {
            return parsePDF(file);
        } else if (filename.endsWith(".docx") || (contentType != null && contentType.contains("word"))) {
            return parseDocx(file);
        } else if (filename.endsWith(".txt") || (contentType != null && contentType.contains("text"))) {
            return parseTxt(file);
        } else {
            throw new UnsupportedOperationException("Unsupported file type. Supported: PDF, DOCX, TXT");
        }
    }

    private String parsePDF(MultipartFile file) throws IOException {
        validateFile(file, 20 * 1024 * 1024, "PDF");

        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted PDFs are not supported");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String parseDocx(MultipartFile file) throws IOException {
        validateFile(file, 5 * 1024 * 1024, "DOCX");

        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            StringBuilder textBuilder = new StringBuilder();

            document.getParagraphs().forEach(paragraph -> {
                if (paragraph.getText() != null && !paragraph.getText().trim().isEmpty()) {
                    textBuilder.append(paragraph.getText()).append("\n");
                }
            });

            document.getTables().forEach(table -> table.getRows().forEach(row -> {
                row.getTableCells().forEach(cell -> {
                    if (cell.getText() != null && !cell.getText().trim().isEmpty()) {
                        textBuilder.append(cell.getText()).append(" ");
                    }
                });
                textBuilder.append("\n");
            }));

            return textBuilder.toString().trim();
        }
    }

    private String parseTxt(MultipartFile file) throws IOException {
        validateFile(file, 5 * 1024 * 1024, "TXT");
        return new String(file.getBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
    }

    private void validateFile(MultipartFile file, long maxSize, String fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fileType + " file is empty or null");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(fileType + " file exceeds size limit of " + (maxSize / (1024 * 1024)) + " MB");
        }
    }


    public String saveUploadedFile(MultipartFile file, String uploadDir) throws IOException {
        Path uploadPath = Path.of(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = System.currentTimeMillis() + "_" +
                (originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "contract");

        Path filePath = uploadPath.resolve(safeFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }
}
