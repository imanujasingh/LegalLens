package com.contractGuard.LegalLens.service;

import com.contractGuard.LegalLens.model.entity.ContractEntity;
import com.contractGuard.LegalLens.model.entity.ClauseAnalysisEntity;
import com.contractGuard.LegalLens.model.entity.ClauseChange;
import com.contractGuard.LegalLens.model.entity.ClauseEntity;
import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
import com.contractGuard.LegalLens.model.enums.ChangeImpact;
import com.contractGuard.LegalLens.model.enums.AnalysisStatus;
import com.contractGuard.LegalLens.model.enums.OverallAssessment;
import com.contractGuard.LegalLens.model.enums.RiskLevel;
import com.contractGuard.LegalLens.repository.ClauseAnalysisRepository;
import com.contractGuard.LegalLens.repository.ClauseChangeRepository;
import com.contractGuard.LegalLens.repository.ClauseRepository;
import com.contractGuard.LegalLens.repository.ContractRepository;
import com.contractGuard.LegalLens.repository.PartyProfileRepository;
import com.contractGuard.LegalLens.service.ai.AIService;
import com.contractGuard.LegalLens.service.parser.DocumentParserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAnalysisWorker {

    private final DocumentParserService documentParserService;
    private final ClauseExtractionService clauseExtractionService;
    private final AIService aiService;
    private final RiskEvaluationService riskEvaluationService;
    private final ContractRepository contractRepository;
    private final PartyProfileRepository partyProfileRepository;
    private final ClauseRepository clauseRepository;
    private final ClauseAnalysisRepository clauseAnalysisRepository;
    private final ClauseChangeRepository clauseChangeRepository;
    private final ContractStatusService contractStatusService;

    @Async("contractAnalysisExecutor")
    @Transactional
    public void analyzeContractAsync(Long contractId, byte[] fileBytes, Long partyProfileId) {
        log.info("Background analysis for contract={} started", contractId);

        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalStateException("Contract not found"));

        PartyProfileEntity profile = partyProfileRepository.findById(partyProfileId)
                .orElseThrow(() -> new IllegalStateException("Party profile not found"));

        try {
            contractStatusService.updateStatus(contract, AnalysisStatus.PARSING, 10);

            String parsedText = documentParserService.parseBytes(
                    fileBytes,
                    contract.getOriginalFilename(),
                    contract.getContentType()
            );

            contract.setParsedText(parsedText);
            contractRepository.save(contract);

            contractStatusService.updateStatus(contract, AnalysisStatus.ANALYZING, 40);
            List<ClauseEntity> newClauses = clauseRepository.saveAll(
                    clauseExtractionService.extractClauses(parsedText, contract)
            );
            AnalysisInput analysisInput = prepareAnalysisInput(contract, newClauses);
            String aiResult = analyzeOnlyChangedContent(analysisInput, parsedText, profile);

            contractStatusService.updateStatus(contract, AnalysisStatus.ANALYZING, 80);

            var riskResult = riskEvaluationService.evaluateRisk(aiResult);
            persistClauseAnalyses(contract, profile, newClauses, analysisInput, aiResult, riskResult.getRiskScore());

            contract.setRiskScore(resolveContractRiskScore(contract.getId(), riskResult.getRiskScore()));
            contract.setAiSummary(aiResult);
            contract.setChangeSummary(analysisInput.changeSummary());

            contractStatusService.updateStatus(contract, AnalysisStatus.COMPLETED, 100);

            log.info("Analysis completed for contractId={}, risk={}",
                    contractId, riskResult.getRiskLevel());

        } catch (Exception e) {
            log.error("Analysis failed for contractId={}: {}", contractId, e.getMessage(), e);

            contract.setFailureReason(e.getMessage());
            contractStatusService.updateStatus(contract, AnalysisStatus.FAILED, 0);
        }
    }

    private AnalysisInput prepareAnalysisInput(ContractEntity contract, List<ClauseEntity> newClauses) {
        if (contract.getParentContract() == null) {
            return AnalysisInput.initial(newClauses);
        }

        List<ClauseEntity> oldClauses = clauseRepository.findByContractIdOrdered(contract.getParentContract().getId());
        Map<String, ClauseEntity> oldByHash = new HashMap<>();
        Map<Integer, ClauseEntity> oldByNumber = new HashMap<>();
        oldClauses.forEach(oldClause -> {
            oldByHash.put(oldClause.getTextHash(), oldClause);
            oldByNumber.put(oldClause.getClauseNumber(), oldClause);
        });

        Set<Long> matchedOldClauseIds = new HashSet<>();
        List<ClauseEntity> changedClauses = new ArrayList<>();
        Map<Long, Long> reusedAnalysisByNewClauseId = new HashMap<>();

        for (ClauseEntity newClause : newClauses) {
            ClauseEntity sameHash = oldByHash.get(newClause.getTextHash());
            if (sameHash != null) {
                matchedOldClauseIds.add(sameHash.getId());
                reusedAnalysisByNewClauseId.put(newClause.getId(), sameHash.getId());
                saveChange(contract, newClause, sameHash, "UNCHANGED", ChangeImpact.NEUTRAL);
                continue;
            }

            ClauseEntity sameNumber = oldByNumber.get(newClause.getClauseNumber());
            if (sameNumber != null) {
                matchedOldClauseIds.add(sameNumber.getId());
                changedClauses.add(newClause);
                saveChange(contract, newClause, sameNumber, "MODIFIED", ChangeImpact.NEUTRAL);
            } else {
                changedClauses.add(newClause);
                saveChange(contract, newClause, null, "ADDED", ChangeImpact.NEUTRAL);
            }
        }

        oldClauses.stream()
                .filter(oldClause -> !matchedOldClauseIds.contains(oldClause.getId()))
                .forEach(oldClause -> saveChange(contract, null, oldClause, "REMOVED", ChangeImpact.NEUTRAL));

        return new AnalysisInput(true, changedClauses, reusedAnalysisByNewClauseId);
    }

    private String analyzeOnlyChangedContent(AnalysisInput analysisInput, String parsedText, PartyProfileEntity profile) {
        if (!analysisInput.hasParentVersion()) {
            return aiService.analyzeContract(parsedText, profile);
        }

        if (analysisInput.changedClauses().isEmpty()) {
            return "No added or modified clauses were detected. Existing clause analysis was reused for unchanged clauses, and removed clauses were recorded without a new LLM call.";
        }

        StringBuilder changedText = new StringBuilder();
        changedText.append("Analyze only these added or modified clauses from a revised contract. ");
        changedText.append("Unchanged clauses have been reused from the previous version.\n\n");
        analysisInput.changedClauses().stream()
                .sorted(Comparator.comparing(ClauseEntity::getClauseNumber))
                .forEach(clause -> changedText
                        .append(clause.getClauseNumber())
                        .append(". ")
                        .append(clause.getOriginalText())
                        .append("\n\n"));
        return aiService.analyzeContract(changedText.toString(), profile);
    }

    private void persistClauseAnalyses(ContractEntity contract,
                                       PartyProfileEntity profile,
                                       List<ClauseEntity> newClauses,
                                       AnalysisInput analysisInput,
                                       String aiResult,
                                       Double changedRiskScore) {
        for (ClauseEntity clause : newClauses) {
            Long oldClauseId = analysisInput.reusedAnalysisByNewClauseId().get(clause.getId());
            if (oldClauseId != null) {
                clauseAnalysisRepository.findByClauseId(oldClauseId)
                        .ifPresent(oldClauseAnalysis -> cloneAnalysis(clause, profile, oldClauseAnalysis));
                continue;
            }

            Double clauseRiskScore = changedRiskScore != null ? changedRiskScore : 0.0;
            saveAnalysis(clause, profile, aiResult, clauseRiskScore);
        }
    }

    private void cloneAnalysis(ClauseEntity newClause, PartyProfileEntity profile, ClauseAnalysisEntity oldAnalysis) {
        ClauseAnalysisEntity clone = new ClauseAnalysisEntity();
        clone.setClause(newClause);
        clone.setPartyProfile(profile);
        clone.setBenefits(oldAnalysis.getBenefits());
        clone.setRisks(oldAnalysis.getRisks());
        clone.setRedFlags(new ArrayList<>(oldAnalysis.getRedFlags()));
        clone.setSuggestions(oldAnalysis.getSuggestions());
        clone.setQuestions(oldAnalysis.getQuestions());
        clone.setRiskScore(oldAnalysis.getRiskScore());
        clone.setOverallAssessment(oldAnalysis.getOverallAssessment());
        clone.setRawAnalysis(oldAnalysis.getRawAnalysis());
        clone.setAiModelUsed(oldAnalysis.getAiModelUsed());
        clauseAnalysisRepository.save(clone);
    }

    private void saveAnalysis(ClauseEntity clause, PartyProfileEntity profile, String aiResult, Double riskScore) {
        ClauseAnalysisEntity analysis = new ClauseAnalysisEntity();
        analysis.setClause(clause);
        analysis.setPartyProfile(profile);
        analysis.setRiskScore(riskScore);
        analysis.setOverallAssessment(resolveAssessment(RiskLevel.fromScore(riskScore)));
        analysis.setRawAnalysis(Map.of(
                "summary", aiResult,
                "source", "LLM_CHANGED_CLAUSES_ONLY"
        ));
        analysis.setAiModelUsed("gpt-4o");
        clauseAnalysisRepository.save(analysis);
    }

    private void saveChange(ContractEntity contract,
                            ClauseEntity newClause,
                            ClauseEntity oldClause,
                            String changeType,
                            ChangeImpact impact) {
        ClauseChange change = new ClauseChange();
        change.setContract(contract);
        change.setClause(newClause);
        change.setClauseNumber(newClause != null ? newClause.getClauseNumber() : oldClause.getClauseNumber());
        change.setClauseType(newClause != null && newClause.getClauseType() != null
                ? newClause.getClauseType().name()
                : oldClause.getClauseType() != null ? oldClause.getClauseType().name() : null);
        change.setOldText(oldClause != null ? oldClause.getOriginalText() : null);
        change.setNewText(newClause != null ? newClause.getOriginalText() : null);
        change.setChangeType(changeType);
        change.setImpact(impact);
        clauseChangeRepository.save(change);
    }

    private Double resolveContractRiskScore(Long contractId, Double fallbackScore) {
        Double averageScore = clauseAnalysisRepository.findAverageRiskScoreByContractId(contractId);
        return averageScore != null ? averageScore : fallbackScore;
    }

    private OverallAssessment resolveAssessment(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL, HIGH -> OverallAssessment.VERY_UNFAVORABLE;
            case MEDIUM -> OverallAssessment.UNFAVORABLE;
            case LOW -> OverallAssessment.NEUTRAL;
            case NONE -> OverallAssessment.FAVORABLE;
        };
    }

    private record AnalysisInput(boolean hasParentVersion,
                                 List<ClauseEntity> changedClauses,
                                 Map<Long, Long> reusedAnalysisByNewClauseId) {
        static AnalysisInput initial(List<ClauseEntity> clauses) {
            return new AnalysisInput(false, clauses, Map.of());
        }

        String changeSummary() {
            if (!hasParentVersion()) {
                return "Initial contract analysis";
            }
            return changedClauses.size() + " added or modified clauses require fresh LLM analysis; unchanged clauses reuse prior analysis.";
        }
    }
}
