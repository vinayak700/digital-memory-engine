package com.memory.context.engine.domain.intelligence;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Text similarity algorithms for matching queries to memory content.
 * No ML models required - uses classical NLP techniques.
 */
@Service
public class TextSimilarityService {

    /**
     * Calculate Jaccard similarity between two texts.
     * Range: 0.0 (no overlap) to 1.0 (identical)
     */
    public double jaccardSimilarity(String text1, String text2) {
        Set<String> set1 = tokenize(text1);
        Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        if (set1.isEmpty() || set2.isEmpty())
            return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Calculate cosine similarity using TF-IDF-like weights.
     */
    public double cosineSimilarity(String text1, String text2) {
        Map<String, Double> vec1 = getTermFrequency(text1);
        Map<String, Double> vec2 = getTermFrequency(text2);

        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vec1.keySet());
        allTerms.addAll(vec2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : allTerms) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0)
            return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Find the most relevant sentence from text given a query.
     */
    public String findMostRelevantSentence(String text, String query) {
        String[] sentences = text.split("[.!?]+");
        String bestSentence = "";
        double bestScore = 0.0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() < 10)
                continue;

            double score = cosineSimilarity(query, sentence);
            if (score > bestScore) {
                bestScore = score;
                bestSentence = sentence;
            }
        }

        // If no good match, return first sentence
        if (bestSentence.isEmpty() && sentences.length > 0) {
            bestSentence = sentences[0].trim();
        }

        // Truncate if too long
        if (bestSentence.length() > 200) {
            bestSentence = bestSentence.substring(0, 197) + "...";
        }

        return bestSentence;
    }

    /**
     * Calculate relevance score between query and memory content.
     * Combines multiple signals.
     */
    public double calculateRelevanceScore(String query, String title, String content, List<String> keywords) {
        double titleScore = cosineSimilarity(query, title) * 2.0; // Title matches weighted higher
        double contentScore = cosineSimilarity(query, content);

        // Keyword match bonus
        double keywordBonus = 0.0;
        if (keywords != null) {
            Set<String> queryTokens = tokenize(query);
            for (String keyword : keywords) {
                if (queryTokens.contains(keyword.toLowerCase())) {
                    keywordBonus += 0.2;
                }
            }
        }

        return Math.min(1.0, (titleScore + contentScore + keywordBonus) / 3.0);
    }

    /**
     * Tokenize text into lowercase words.
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank())
            return Set.of();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 2)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Calculate term frequency for a text.
     */
    private Map<String, Double> getTermFrequency(String text) {
        Map<String, Double> tf = new HashMap<>();
        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            if (word.length() > 2) {
                tf.merge(word, 1.0, Double::sum);
            }
        }
        // Normalize
        double total = tf.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            tf.replaceAll((k, v) -> v / total);
        }
        return tf;
    }
}
