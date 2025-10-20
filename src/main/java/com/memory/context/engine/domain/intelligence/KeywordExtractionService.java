package com.memory.context.engine.domain.intelligence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Keyword Extraction Service using RAKE (Rapid Automatic Keyword Extraction)
 * algorithm.
 * Extracts important keywords and phrases from text without using ML models.
 */
@Slf4j
@Service
public class KeywordExtractionService {

    // Common English stop words to filter out (deduplicated and sorted)
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "also", "am", "an",
            "and", "are", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "but", "by", "can", "come", "could", "dare", "did", "do", "does",
            "doing", "don", "down", "during", "each", "few", "for", "from", "further",
            "get", "go", "got", "had", "has", "have", "having", "he", "her", "here",
            "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into",
            "is", "it", "its", "itself", "just", "know", "like", "look", "make", "may",
            "me", "might", "more", "most", "must", "my", "myself", "need", "no", "nor",
            "not", "now", "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own", "s", "same", "see", "shall",
            "she", "should", "so", "some", "such", "t", "take", "than", "that", "the",
            "their", "theirs", "them", "themselves", "then", "there", "these", "they",
            "think", "this", "those", "through", "to", "too", "under", "until", "up",
            "use", "used", "very", "want", "was", "we", "were", "what", "when", "where",
            "which", "while", "who", "whom", "why", "will", "with", "would", "you",
            "your", "yours", "yourself", "yourselves");

    private static final Pattern SENTENCE_DELIMITERS = Pattern.compile("[.!?;:\\n\\r]+");
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * Extract keywords from text using RAKE algorithm.
     * Returns top N keywords sorted by score.
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 1. Split into sentences
        String[] sentences = SENTENCE_DELIMITERS.split(text.toLowerCase());

        // 2. Extract candidate keywords (phrases between stop words)
        List<List<String>> phrases = new ArrayList<>();
        for (String sentence : sentences) {
            phrases.addAll(extractPhrases(sentence));
        }

        // 3. Calculate word scores (degree/frequency)
        Map<String, Integer> wordFrequency = new HashMap<>();
        Map<String, Integer> wordDegree = new HashMap<>();

        for (List<String> phrase : phrases) {
            int degree = phrase.size() - 1;
            for (String word : phrase) {
                wordFrequency.merge(word, 1, (a, b) -> a + b);
                wordDegree.merge(word, degree, (a, b) -> a + b);
            }
        }

        Map<String, Double> wordScore = new HashMap<>();
        for (String word : wordFrequency.keySet()) {
            double score = (wordDegree.get(word) + wordFrequency.get(word)) / (double) wordFrequency.get(word);
            wordScore.put(word, score);
        }

        // 4. Calculate phrase scores
        Map<String, Double> phraseScores = new HashMap<>();
        for (List<String> phrase : phrases) {
            String phraseStr = String.join(" ", phrase);
            double score = phrase.stream()
                    .mapToDouble(w -> wordScore.getOrDefault(w, 0.0))
                    .sum();
            phraseScores.merge(phraseStr, score, (a, b) -> Math.max(a, b));
        }

        // 5. Return top N keywords
        return phraseScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Extract phrases (sequences of non-stop words)
     */
    private List<List<String>> extractPhrases(String sentence) {
        List<List<String>> phrases = new ArrayList<>();
        List<String> currentPhrase = new ArrayList<>();

        var matcher = WORD_PATTERN.matcher(sentence);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (STOP_WORDS.contains(word)) {
                if (!currentPhrase.isEmpty()) {
                    phrases.add(new ArrayList<>(currentPhrase));
                    currentPhrase.clear();
                }
            } else if (word.length() > 2) {
                currentPhrase.add(word);
            }
        }

        if (!currentPhrase.isEmpty()) {
            phrases.add(currentPhrase);
        }

        return phrases;
    }

    /**
     * Extract keywords and return as a Map for storing in context field.
     */
    public Map<String, Object> extractContextMetadata(String title, String content) {
        Map<String, Object> context = new HashMap<>();

        // Extract keywords from title (higher weight)
        List<String> titleKeywords = extractKeywords(title, 3);

        // Extract keywords from content
        List<String> contentKeywords = extractKeywords(content, 7);

        // Combine and deduplicate
        Set<String> allKeywords = new LinkedHashSet<>(titleKeywords);
        allKeywords.addAll(contentKeywords);

        context.put("keywords", new ArrayList<>(allKeywords));
        context.put("wordCount", content.split("\\s+").length);
        context.put("sentenceCount", SENTENCE_DELIMITERS.split(content).length);
        context.put("extractedAt", java.time.Instant.now().toString());

        return context;
    }
}
