package com.contractGuard.LegalLens.service.ai;

import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PromptBuilder {

    private static final String NOT_SPECIFIED = "Not specified";

    public Map<String, Object> buildContractAnalysisParameters(String contractText, PartyProfileEntity partyProfile) {
        log.info("Inside buildContractAnalysisParameters");
        Map<String, Object> params = new HashMap<>();

        params.put("partyName", partyProfile.getPartyName());
        params.put("partyRole", partyProfile.getPartyRole().getDisplayName());
        params.put("industry", defaultIfNull(partyProfile.getIndustry()));
        params.put("jurisdiction", defaultIfNull(partyProfile.getJurisdiction()));
        params.put("contractText", contractText);

        log.debug("Built contract analysis parameters for {}", partyProfile.getPartyName());
        return params;
    }

    private String defaultIfNull(String value) {
        return value != null ? value : NOT_SPECIFIED;
    }
}
