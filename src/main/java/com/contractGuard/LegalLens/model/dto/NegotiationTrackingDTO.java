package com.contractGuard.LegalLens.model.dto;


import com.contractGuard.LegalLens.model.enums.NegotiationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NegotiationTrackingDTO {

    private Long id;
    private Long contractId;
    private Integer roundNumber;
    private NegotiationStatus status;

    private Map<String, String> changesRequested;
    private Map<String, String> changesAccepted;
    private Map<String, String> changesRejected;

    private String counterpartyComments;
    private String ourResponse;
    private String summary;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String counterpartyEmail;
    private String sentBy;

    @Data
    public static class CreateRequest {
        private Long contractId;
        private Map<String, String> changesRequested;
        private String counterpartyEmail;
        private String notes;
    }

    @Data
    public static class UpdateRequest {
        private NegotiationStatus status;
        private Map<String, String> changesAccepted;
        private Map<String, String> changesRejected;
        private String counterpartyComments;
        private String ourResponse;
    }
}
