package com.contractGuard.LegalLens.service.parser;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class DocumentParserService {

    public String parseBytes(byte[] bytes, String filename, String contentType)
            throws IOException {

        String name = filename != null ? filename.toLowerCase() : "";

        if (name.endsWith(".pdf") ||
                (contentType != null && contentType.contains("pdf"))) {
            return parsePDFBytes(bytes);
        } else if (name.endsWith(".docx") ||
                (contentType != null && contentType.contains("word"))) {
            return parseDocxBytes(bytes);
        } else if (name.endsWith(".txt") ||
                (contentType != null && contentType.contains("text"))) {
            return new String(bytes, StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported file type. Supported: PDF, DOCX, TXT"
            );
        }
    }

    private String parsePDFBytes(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "Encrypted PDFs are not supported"
                );
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String parseDocxBytes(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(is)) {

            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(p -> {
                if (p.getText() != null && !p.getText().trim().isEmpty()) {
                    text.append(p.getText()).append("\n");
                }
            });
            return text.toString().trim();
        }
    }

}
