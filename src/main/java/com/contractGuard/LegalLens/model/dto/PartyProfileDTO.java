package com.contractGuard.LegalLens.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PartyProfileDTO {

    private Long id;
    private String partyName;
    private String partyRole;
    private String industry;
    private String jurisdiction;

    private List<String> riskTolerance;
    private Map<String, Integer> negotiationPriorities;
    private List<String> dealBreakers;
    private List<String> preferredTerms;

    @Data
    public static class CreateRequest {
        private String partyName;
        private String partyRole;
        private String industry;
        private String jurisdiction;
        private List<String> riskTolerance;
        private Map<String, Integer> negotiationPriorities;
        private List<String> dealBreakers;
        private List<String> preferredTerms;
    }

    @Data
    public static class UpdateRequest {
        private String partyRole;
        private String industry;
        private String jurisdiction;
        private List<String> riskTolerance;
        private Map<String, Integer> negotiationPriorities;
        private List<String> dealBreakers;
        private List<String> preferredTerms;
    }
}
