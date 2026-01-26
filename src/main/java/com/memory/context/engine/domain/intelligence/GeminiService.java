package com.memory.context.engine.domain.intelligence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.memory.context.engine.domain.intelligence.cache.SemanticCacheResult;
import com.memory.context.engine.domain.intelligence.cache.SemanticCacheService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service to interact with Google's Gemini API.
 * Uses semantic caching with Jaccard similarity for intelligent cache matching.
 */
@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SemanticCacheService semanticCacheService;

    private static final String ANSWERS_CACHE = "gemini-answers";
    private static final String SEARCH_TERMS_CACHE = "gemini-search-terms";

    public GeminiService(SemanticCacheService semanticCacheService) {
        this.semanticCacheService = semanticCacheService;
    }

    /**
     * Generate an answer using Gemini API with semantic caching.
     * Similar questions will return cached answers based on keyword similarity.
     */
    public String generateAnswer(String question, List<String> memoryContexts) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is missing. Returning fallback response.");
            return "I am unable to generate an intelligent answer because the AI service is not configured.";
        }

        // Create a composite cache key that includes memory context hash
        String cacheQuestion = question + "_ctx" + memoryContexts.hashCode();

        // Try semantic cache lookup first
        SemanticCacheResult cacheResult = semanticCacheService.lookup(ANSWERS_CACHE, cacheQuestion);
        if (cacheResult.isHit()) {
            log.info("Using semantically cached answer (similarity: {})",
                    String.format("%.2f", cacheResult.getSimilarityScore()));
            return cacheResult.getCachedValue();
        }

        // Cache miss - generate new answer
        String prompt = buildPrompt(question, memoryContexts);
        String answer = callGemini(prompt, false);

        // Store in semantic cache (unless it's an error response)
        if (!answer.contains("Sorry") && !answer.contains("I don't have enough")) {
            semanticCacheService.store(ANSWERS_CACHE, cacheQuestion, answer, 1.0);
        }

        return answer;
    }

    /**
     * Extracts search terms from the question using the LLM to understand intent.
     * Uses semantic caching - similar questions return cached search terms.
     */
    public List<String> extractSearchTerms(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return Collections.emptyList();
        }

        // Try semantic cache lookup first
        SemanticCacheResult cacheResult = semanticCacheService.lookup(SEARCH_TERMS_CACHE, question);
        if (cacheResult.isHit()) {
            log.info("Using semantically cached search terms (similarity: {})",
                    String.format("%.2f", cacheResult.getSimilarityScore()));
            // Parse cached value back to list
            String cached = cacheResult.getCachedValue();
            return new ArrayList<>(Arrays.asList(cached.split("\\|")));
        }

        // Cache miss - extract new search terms
        String prompt = """
                You are an intelligent memory retrieval assistant.
                Your goal is to generate search terms that will find RELEVANT memories for the user's question, even if they don't use the exact same words.

                Think laterally and associatively.
                - If the user asks about a specific concept (e.g., "list"), include related higher-level concepts (e.g., "collections", "streams", "arrays") and connected topics.
                - Include synonyms, technical terms, and broader contexts.

                USER QUESTION:
                %s

                OUTPUT FORMAT:
                Return ONLY a pipe-separated list of terms. Example: term1|term 2|term3
                Do not include any other text.
                """
                .formatted(question);

        String response = callGemini(prompt, true);
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }

        // Clean up response and split
        List<String> terms = new ArrayList<>(Arrays.stream(response.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());

        // Store in semantic cache
        if (!terms.isEmpty()) {
            semanticCacheService.store(SEARCH_TERMS_CACHE, question, String.join("|", terms), 1.0);
        }

        return terms;
    }

    private String callGemini(String prompt, boolean isSearchQuery) {
        int maxRetries = 3;
        long retryDelay = 1000; // Start with 1 second

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                GeminiRequest request = new GeminiRequest();
                request.setContents(Collections.singletonList(
                        new Content(Collections.singletonList(
                                new Part(prompt)))));

                HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

                String url = apiUrl + "?key=" + apiKey;
                GeminiResponse response = restTemplate.postForObject(url, entity, GeminiResponse.class);

                if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                    Candidate candidate = response.getCandidates().get(0);
                    if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                        return candidate.getContent().getParts().get(0).getText().trim();
                    }
                }

                return isSearchQuery ? "" : "I couldn't generate an answer from the AI model.";
            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable
                    | org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API overloaded or rate limited (Attempt {}/{}). Retrying in {} ms...", attempt,
                        maxRetries, retryDelay);
                if (attempt == maxRetries) {
                    log.error("Gemini API failed after {} attempts.", maxRetries, e);
                    return isSearchQuery ? ""
                            : "Sorry, the AI service is currently overloaded. Please try again later.";
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return isSearchQuery ? "" : "Operation interrupted.";
                }
                retryDelay *= 2; // Exponential backoff
            } catch (Exception e) {
                log.error("Error calling Gemini API: ", e);
                return isSearchQuery ? "" : "Sorry, I encountered an error while communicating with the AI service.";
            }
        }
        return isSearchQuery ? "" : "Sorry, I was unable to get a response from the AI service.";
    }

    private String buildPrompt(String question, List<String> memoryContexts) {
        StringBuilder contextBuilder = new StringBuilder();
        for (String memory : memoryContexts) {
            contextBuilder.append("- ").append(memory).append("\n");
        }

        return """
                You are a helpful personal memory assistant. You have access to the user's digital memories.
                Answer the user's question based ONLY on the provided memories.
                If the answer is not in the memories, say "I don't have enough information in your memories to answer that."
                Do not make up information.

                USER MEMORIES:
                %s

                USER QUESTION:
                %s

                ANSWER:
                """
                .formatted(contextBuilder.toString(), question);
    }

    // --- DTOs for Gemini API ---

    @Data
    private static class GeminiRequest {
        private List<Content> contents;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Candidate {
        private Content content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class Content {
        private List<Part> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class Part {
        private String text;
    }
}
