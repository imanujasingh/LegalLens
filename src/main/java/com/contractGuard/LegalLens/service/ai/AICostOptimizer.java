//package com.contractGuard.LegalLens.service.ai;
//
//import com.contractGuard.LegalLens.model.enums.AiModelTier;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@Slf4j
//public class AICostOptimizer {
//
//    @Value("${legal-lens.ai.cost-optimization.enabled:true}")
//    private boolean costOptimizationEnabled;
//
//    private static final List<String> COMPLEX_CLAUSE_TYPES = List.of(
//            "INDEMNIFICATION",
//            "LIMITATION_OF_LIABILITY",
//            "INTELLECTUAL_PROPERTY",
//            "DATA_PRIVACY"
//    );
//
//    private static final List<String> SIMPLE_CLAUSE_TYPES = List.of(
//            "DEFINITIONS",
//            "NOTICES",
//            "HEADINGS",
//            "COUNTERPARTS"
//    );
//
//    @Cacheable(
//            value = "ai-model-tier",
//            key = "T(java.util.Objects).hash(#clauseText, #clauseType)"
//    )
//    public AiModelTier selectModelTier(String clauseText, String clauseType) {
//
//        if (!costOptimizationEnabled) {
//            return AiModelTier.COMPLEX; // safest default
//        }
//
//        boolean complex = isComplexClause(clauseText, clauseType);
//        boolean longClause = clauseText.length() > 1200;
//
//        if (complex) {
//            log.debug("Selected COMPLEX tier for clause type: {}", clauseType);
//            return AiModelTier.COMPLEX;
//        }
//
//        if (longClause) {
//            log.debug("Selected STANDARD tier due to length");
//            return AiModelTier.STANDARD;
//        }
//
//        log.debug("Selected FAST tier");
//        return AiModelTier.FAST;
//    }
//
//    private boolean isComplexClause(String clauseText, String clauseType) {
//
//        String normalizedType = clauseType.toUpperCase();
//
//        if (COMPLEX_CLAUSE_TYPES.contains(normalizedType)) {
//            return true;
//        }
//
//        if (SIMPLE_CLAUSE_TYPES.contains(normalizedType)) {
//            return false;
//        }
//
//        String lowerText = clauseText.toLowerCase();
//
//        return List.of(
//                "shall indemnify",
//                "hold harmless",
//                "limitation of liability",
//                "consequential damages",
//                "intellectual property",
//                "confidential information",
//                "data protection",
//                "gdpr",
//                "personal data"
//        ).stream().anyMatch(lowerText::contains);
//    }
//}
