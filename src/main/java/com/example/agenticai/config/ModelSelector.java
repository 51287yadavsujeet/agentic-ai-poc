package com.example.agenticai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Determines which LLM model to use based on the user message content.
 * Routes to Gemini for: Math, History, Geography, Medical Science
 * Routes to OpenAI for: Everything else (default)
 */
@Component
public class ModelSelector {

    private static final Logger log = LoggerFactory.getLogger(ModelSelector.class);

    private static final String[] GEMINI_KEYWORDS = {
            // Math related
            "math", "mathematics", "algebra", "geometry", "calculus", "trigonometry",
            "equation", "formula", "calculate", "computation", "numerical",
            
            // History related
            "history", "historical", "ancient", "medieval", "renaissance", "century",
            "empire", "civilization", "dynasty", "epoch", "era",
            
            // Geography related
            "geography", "geographical", "map", "location", "country", "city", "region",
            "continent", "mountain", "river", "ocean", "climate", "terrain", "coordinates",
            
            // Medical/Science related
            "medical", "medicine", "disease", "symptom", "treatment", "diagnosis",
            "anatomy", "biology", "pathology", "therapy", "cure", "health", "clinical"
    };

    public enum ModelType {
        OPENAI("gpt-4o-mini"),
        GEMINI("gemini-2.0-flash");

        public final String defaultModel;

        ModelType(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }

    /**
     * Analyzes the user message and returns the model type to use
     */
    public ModelType selectModel(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            log.info("[MODEL_SELECTOR] Empty message, using default OPENAI");
            return ModelType.OPENAI;
        }

        String messageLower = userMessage.toLowerCase();
        
        // Check for Gemini-specific keywords
        for (String keyword : GEMINI_KEYWORDS) {
            if (messageLower.contains(keyword)) {
                log.info("[MODEL_SELECTOR] Keyword '{}' detected | Selecting GEMINI model", keyword);
                return ModelType.GEMINI;
            }
        }

        // Default to OpenAI
        log.info("[MODEL_SELECTOR] No special keywords detected | Selecting default OPENAI model");
        return ModelType.OPENAI;
    }
}
