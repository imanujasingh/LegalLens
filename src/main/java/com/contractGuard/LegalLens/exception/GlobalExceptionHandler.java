package com.contractGuard.LegalLens.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 503 AI Unavailable ────────────────────────────────────────────────────
    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiUnavailable(AiUnavailableException ex) {
        log.error("AI service unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "AI Service Unavailable", ex.getMessage());
    }

    // ── 404 Contract Not Found ────────────────────────────────────────────────
    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleContractNotFound(ContractNotFoundException ex) {
        log.warn("Contract not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    // ── 409 Duplicate Contract ────────────────────────────────────────────────
    @ExceptionHandler(DuplicateContractException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateContractException ex) {
        log.warn("Duplicate contract upload attempted: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "Duplicate Contract", ex.getMessage());
    }

    // ── 409 Analysis Not Ready ────────────────────────────────────────────────
    @ExceptionHandler(AnalysisNotReadyException.class)
    public ResponseEntity<Map<String, Object>> handleNotReady(AnalysisNotReadyException ex) {
        log.info("Analysis not ready: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "Analysis Not Ready", ex.getMessage());
    }

    // ── 400 Validation Errors (Jakarta Bean Validation) ───────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", details);
        return error(HttpStatus.BAD_REQUEST, "Validation Failed", details);
    }

    // ── 400 Missing Required Request Parameter ────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    // ── 400 Illegal Argument (empty file, empty party name, etc.) ─────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // ── 415 Unsupported File Type ─────────────────────────────────────────────
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedFileType(UnsupportedOperationException ex) {
        log.warn("Unsupported file type: {}", ex.getMessage());
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage());
    }

    // ── 413 File Too Large ────────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "File Too Large",
                "Uploaded file exceeds the maximum allowed size of 20MB.");
    }

    // ── 500 Catch-All ─────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException ex) {
        String message = "Required file part '" + ex.getRequestPartName() + "' is missing";
        log.warn("Missing multipart part: {}", ex.getRequestPartName());
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
