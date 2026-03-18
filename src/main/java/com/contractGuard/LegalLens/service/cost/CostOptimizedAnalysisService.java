//package com.contractGuard.LegalLens.service.cost;
//
//
//import com.contractGuard.LegalLens.model.entity.ClauseAnalysisEntity;
//import com.contractGuard.LegalLens.model.entity.ClauseEntity;
//import com.contractGuard.LegalLens.model.entity.ContractEntity;
//import com.contractGuard.LegalLens.model.entity.PartyProfileEntity;
//import com.contractGuard.LegalLens.service.ai.AICostOptimizer;
//import com.contractGuard.LegalLens.service.ai.AIService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CostOptimizedAnalysisService {
//
//    private final AIService aiService;
//    private final AICostOptimizer costOptimizer;
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    private static final String CACHE_PREFIX = "ai_analysis:";
//    private static final Duration CACHE_TTL = Duration.ofHours(6);
//
//    public List<CompletableFuture<ClauseAnalysisEntity>> analyzeClausesCostOptimized(
//            List<ClauseEntity> clauses,
//            PartyProfileEntity partyProfile) {
//
//        List<CompletableFuture<ClauseAnalysisEntity>> futures = new ArrayList<>();
//
//        for (ClauseEntity clause : clauses) {
//            CompletableFuture<ClauseAnalysisEntity> future =
//                    analyzeClauseCostOptimized(clause, partyProfile);
//            futures.add(future);
//        }
//
//        return futures;
//    }
//
//    public CompletableFuture<ClauseAnalysisEntity> analyzeClauseCostOptimized(
//            ClauseEntity clause,
//            PartyProfileEntity partyProfile) {
//
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                return analyzeWithCostOptimization(clause, partyProfile);
//            } catch (Exception e) {
//                log.error("Cost-optimized analysis failed for clause {}: {}",
//                        clause.getClauseNumber(), e.getMessage());
//                throw new RuntimeException("Analysis failed", e);
//            }
//        });
//    }
//
//    @Cacheable(value = "aiResponses", key = "#clause.textHash + '-' + #partyProfile.id")
//    public ClauseAnalysisEntity analyzeWithCostOptimization(ClauseEntity clause,
//                                                            PartyProfileEntity partyProfile) {
//
//        String clauseHash = generateClauseHash(clause, partyProfile);
//
//        // 1. Check cache first
//        ClauseAnalysisEntity cached = getCachedAnalysis(clauseHash);
//        if (cached != null) {
//            log.debug("Cache hit for clause {}", clause.getClauseNumber());
//            cached.setId(null); // Clear ID for new save
//            return cached;
//        }
//
//        // 2. Estimate cost and decide on strategy
//        double estimatedCost = aiService.estimateCost(
//                clause.getOriginalText(),
//                "gpt-4-turbo-preview"
//        );
//
//        boolean useCache = costOptimizer.shouldUseCachedAnalysis(clauseHash, estimatedCost);
//        if (useCache) {
//            // Try to find similar cached analysis
//            ClauseAnalysisEntity similar = findSimilarCachedAnalysis(clause, partyProfile);
//            if (similar != null) {
//                log.debug("Using similar cached analysis for clause {}", clause.getClauseNumber());
//                cacheAnalysis(clauseHash, similar);
//                return similar;
//            }
//        }
//
//        // 3. Use tiered analysis approach
//        return performTieredAnalysis(clause, partyProfile, clauseHash);
//    }
//
//    private ClauseAnalysisEntity performTieredAnalysis(ClauseEntity clause,
//                                                       PartyProfileEntity partyProfile,
//                                                       String clauseHash) {
//
//        String clauseType = clause.getClauseType().toString();
//        String clauseText = clause.getOriginalText();
//
//        // Tier 1: Simple rule-based checks for common patterns
//        if (canUseRuleBasedAnalysis(clauseText, clauseType)) {
//            log.debug("Using rule-based analysis for clause {}", clause.getClauseNumber());
//            ClauseAnalysisEntity analysis = performRuleBasedAnalysis(clause, partyProfile);
//            cacheAnalysis(clauseHash, analysis);
//            return analysis;
//        }
//
//        // Tier 2: Use cheaper AI model for standard clauses
//        if (shouldUseCheaperModel(clauseText, clauseType)) {
//            log.debug("Using cheaper AI model for clause {}", clause.getClauseNumber());
//            // The AI service will select cheaper model via cost optimizer
//        }
//
//        // Tier 3: Full AI analysis with cost tracking
//        return performFullAIAnalysis(clause, partyProfile, clauseHash);
//    }
//
//    private boolean canUseRuleBasedAnalysis(String clauseText, String clauseType) {
//        // Simple rule-based analysis for very standard clauses
//        List<String> standardPatterns = List.of(
//                "entire agreement",
//                "severability",
//                "notices",
//                "headings",
//                "counterparts"
//        );
//
//        String lowerText = clauseText.toLowerCase();
//        for (String pattern : standardPatterns) {
//            if (lowerText.contains(pattern)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    private ClauseAnalysisEntity performRuleBasedAnalysis(ClauseEntity clause,
//                                                          PartyProfileEntity partyProfile) {
//
//        ClauseAnalysisEntity analysis = new ClauseAnalysisEntity();
//        analysis.setClause(clause);
//        analysis.setPartyProfile(partyProfile);
//        analysis.setRiskScore(5.0); // Neutral
//        analysis.setOverallAssessment(com.contractGuard.LegalLens.model.enums.OverallAssessment.NEUTRAL);
//        analysis.setAiModelUsed("rule-based");
//
//        Map<String, Object> ruleBasedAnalysis = Map.of(
//                "benefits", List.of("Standard boilerplate clause"),
//                "risks", List.of(Map.of(
//                        "description", "No significant risks identified",
//                        "severity", "LOW",
//                        "impact", "Minimal",
//                        "mitigation", "None required"
//                )),
//                "overallAssessment", "NEUTRAL",
//                "analysisMethod", "rule-based"
//        );
//
//        analysis.setRawAnalysis(ruleBasedAnalysis);
//        return analysis;
//    }
//
//    private boolean shouldUseCheaperModel(String clauseText, String clauseType) {
//        // Use cheaper model for:
//        // 1. Short clauses (< 500 chars)
//        // 2. Standard clauses (definitions, notices, etc.)
//        // 3. Clauses without complex legal terms
//
//        if (clauseText.length() < 500) {
//            return true;
//        }
//
//        List<String> simpleClauseTypes = List.of(
//                "DEFINITIONS",
//                "NOTICES",
//                "HEADINGS",
//                "COUNTERPARTS",
//                "SEVERABILITY",
//                "ENTIRE_AGREEMENT"
//        );
//
//        if (simpleClauseTypes.contains(clauseType.toUpperCase())) {
//            return true;
//        }
//
//        // Check for complex legal terms
//        List<String> complexTerms = List.of(
//                "indemnification",
//                "limitation of liability",
//                "intellectual property",
//                "confidential information",
//                "warranties",
//                "representations"
//        );
//
//        String lowerText = clauseText.toLowerCase();
//        for (String term : complexTerms) {
//            if (lowerText.contains(term)) {
//                return false; // Don't use cheaper model for complex clauses
//            }
//        }
//
//        return true;
//    }
//
//    private ClauseAnalysisEntity performFullAIAnalysis(ClauseEntity clause,
//                                                       PartyProfileEntity partyProfile,
//                                                       String clauseHash) {
//
//        try {
//            String aiResponse = aiService.analyzeClauseFromPerspective(
//                    clause.getOriginalText(),
//                    partyProfile,
//                    clause.getClauseType().getDisplayName()
//            );
//
//            // Parse response and create analysis entity
//            // (This would use the same parsing logic as ClauseService)
//
//            ClauseAnalysisEntity analysis = new ClauseAnalysisEntity();
//            analysis.setClause(clause);
//            analysis.setPartyProfile(partyProfile);
//            analysis.setAiModelUsed(aiService.getLastUsedModel());
//
//            // Cache the result
//            cacheAnalysis(clauseHash, analysis);
//
//            return analysis;
//
//        } catch (Exception e) {
//            log.error("Full AI analysis failed for clause {}: {}",
//                    clause.getClauseNumber(), e.getMessage());
//
//            // Fall back to rule-based
//            return performRuleBasedAnalysis(clause, partyProfile);
//        }
//    }
//
//    private String generateClauseHash(ClauseEntity clause, PartyProfileEntity partyProfile) {
//        String combined = clause.getOriginalText() +
//                clause.getClauseType() +
//                partyProfile.getId() +
//                partyProfile.getPartyRole();
//        return DigestUtils.sha256Hex(combined);
//    }
//
//    private ClauseAnalysisEntity getCachedAnalysis(String clauseHash) {
//        try {
//            return (ClauseAnalysisEntity) redisTemplate.opsForValue()
//                    .get(CACHE_PREFIX + clauseHash);
//        } catch (Exception e) {
//            log.warn("Cache read failed: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    private void cacheAnalysis(String clauseHash, ClauseAnalysisEntity analysis) {
//        try {
//            redisTemplate.opsForValue()
//                    .set(CACHE_PREFIX + clauseHash, analysis, CACHE_TTL);
//            log.debug("Cached analysis for hash: {}", clauseHash);
//        } catch (Exception e) {
//            log.warn("Cache write failed: {}", e.getMessage());
//        }
//    }
//
//    private ClauseAnalysisEntity findSimilarCachedAnalysis(ClauseEntity clause,
//                                                           PartyProfileEntity partyProfile) {
//        // This would implement similarity search in cache
//        // For now, return null (simplified implementation)
//        return null;
//    }
//
//    public double calculateTotalCostEstimate(List<ClauseEntity> clauses,
//                                             PartyProfileEntity partyProfile) {
//        double totalCost = 0.0;
//
//        for (ClauseEntity clause : clauses) {
//            String model = costOptimizer.selectOptimalModel(
//                    clause.getOriginalText(),
//                    clause.getClauseType().toString()
//            );
//
//            int estimatedTokens = clause.getOriginalText().length() / 4;
//            double clauseCost = costOptimizer.calculateEstimatedCost(model, estimatedTokens);
//            totalCost += clauseCost;
//        }
//
//        log.debug("Estimated total cost for {} clauses: ${}",
//                clauses.size(), String.format("%.4f", totalCost));
//
//        return totalCost;
//    }
//}
