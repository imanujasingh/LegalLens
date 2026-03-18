package com.contractGuard.LegalLens.service;

import com.contractGuard.LegalLens.model.dto.RiskEvaluationResult;
import com.contractGuard.LegalLens.model.enums.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RiskEvaluationService {

    public RiskEvaluationResult evaluateRisk(String aiAnalysisText) {
        log.info("Starting risk evaluation from AI analysis text.");
        // 1️⃣ Handle empty AI output safely
        if (aiAnalysisText == null || aiAnalysisText.isBlank()) {
            return new RiskEvaluationResult(RiskLevel.NONE, 0.0, List.of(), List.of());
        }

        // 2️⃣ Normalize text
        String text = aiAnalysisText
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();

        double score = 0.0;
        List<String> redFlags = new ArrayList<>();
        List<String> greenFlags = new ArrayList<>();

        // 3️⃣ Critical / High risk signals
        if (text.contains("critical risk")) {
            score += 4;
            redFlags.add("Critical risk identified");
        }

        if (text.contains("high risk")) {
            score += 3;
            redFlags.add("High risk exposure");
        }

        // 4️⃣ Financial exposure risks
        if (text.contains("uncapped") || text.contains("unlimited liability")) {
            score += 2;
            redFlags.add("Uncapped liability exposure");
        }

        if (text.contains("penalty") || text.contains("liquidated damages")) {
            score += 1;
            redFlags.add("Penalty or liquidated damages present");
        }

        // 5️⃣ Legal obligation risks
        if (text.contains("indemnify") || text.contains("hold harmless")) {
            score += 1.5;
            redFlags.add("Broad indemnification obligation");
        }

        if (text.contains("waiver")) {
            score += 1;
            redFlags.add("Waiver of legal rights");
        }

        // 6️⃣ Termination risks
        if (text.contains("immediate termination")) {
            score += 1;
            redFlags.add("Immediate termination without cure period");
        }

        // 7️⃣ Positive / mitigating signals
        if (text.contains("mutual")) {
            greenFlags.add("Mutual obligations present");
        }

        if (text.contains("industry standard") || text.contains("reasonable")) {
            greenFlags.add("Industry-standard or reasonable terms");
        }

        // 8️⃣ Cap score at 10
        score = Math.min(score, 10.0);

        // 9️⃣ Derive risk level using enum
        RiskLevel riskLevel = RiskLevel.fromScore(score);

        log.debug("Risk evaluation completed: level={}, score={}", riskLevel, score);

        // 🔟 Return structured result
        return new RiskEvaluationResult(
                riskLevel,
                score,
                redFlags,
                greenFlags
        );
    }
}
