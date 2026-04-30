package com.contractGuard.LegalLens.service;

import com.contractGuard.LegalLens.model.entity.ClauseEntity;
import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.enums.ClauseType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClauseExtractionService {

    private static final Pattern NUMBERED_CLAUSE_PATTERN = Pattern.compile(
            "(?ms)^\\s*(\\d+)\\.\\s*([^:\\n]{1,120})\\s*:\\s*(.*?)(?=^\\s*\\d+\\.\\s*[^:\\n]{1,120}\\s*:|\\z)"
    );

    public List<ClauseEntity> extractClauses(String parsedText, ContractEntity contract) {
        List<ClauseEntity> clauses = new ArrayList<>();
        if (parsedText == null || parsedText.isBlank()) {
            return clauses;
        }

        Matcher matcher = NUMBERED_CLAUSE_PATTERN.matcher(parsedText);
        while (matcher.find()) {
            Integer clauseNumber = Integer.parseInt(matcher.group(1));
            String title = matcher.group(2).trim();
            String body = matcher.group(3).trim();
            String originalText = clauseNumber + ". " + title + ":\n" + body;

            ClauseEntity clause = new ClauseEntity();
            clause.setContract(contract);
            clause.setClauseNumber(clauseNumber);
            clause.setClauseType(resolveClauseType(title + " " + body));
            clause.setOriginalText(originalText);
            clause.setExtractedText(body);
            clause.setTextHash(hash(normalize(originalText)));
            clauses.add(clause);
        }

        if (clauses.isEmpty()) {
            ClauseEntity clause = new ClauseEntity();
            clause.setContract(contract);
            clause.setClauseNumber(1);
            clause.setClauseType(ClauseType.GENERAL);
            clause.setOriginalText(parsedText.trim());
            clause.setExtractedText(parsedText.trim());
            clause.setTextHash(hash(normalize(parsedText)));
            clauses.add(clause);
        }

        return clauses;
    }

    private ClauseType resolveClauseType(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("indemn")) return ClauseType.INDEMNIFICATION;
        if (value.contains("liability")) return ClauseType.LIMITATION_OF_LIABILITY;
        if (value.contains("terminat")) return ClauseType.TERMINATION;
        if (value.contains("confidential")) return ClauseType.CONFIDENTIALITY;
        if (value.contains("governing law") || value.contains("dispute") || value.contains("arbitration")) {
            return ClauseType.DISPUTE_RESOLUTION;
        }
        if (value.contains("payment") || value.contains("fees") || value.contains("invoice")) return ClauseType.PAYMENT_TERMS;
        if (value.contains("intellectual property") || value.contains("deliverable")) return ClauseType.INTELLECTUAL_PROPERTY;
        if (value.contains("data")) return ClauseType.DATA_PRIVACY;
        if (value.contains("service level")) return ClauseType.SERVICE_LEVEL_AGREEMENT;
        if (value.contains("services") || value.contains("scope")) return ClauseType.SCOPE_OF_WORK;
        if (value.contains("waiver")) return ClauseType.WAIVER;
        return ClauseType.GENERAL;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
