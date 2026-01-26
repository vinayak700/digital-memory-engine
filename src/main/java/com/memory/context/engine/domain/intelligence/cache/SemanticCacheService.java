package com.memory.context.engine.domain.intelligence.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.memory.context.engine.domain.intelligence.KeywordExtractionService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Semantic Cache Service with high-performance optimizations.
 * 
 * Performance optimizations:
 * 1. Caffeine L1 in-memory cache (sub-millisecond lookups)
 * 2. Batch Redis multiGet (single network round-trip)
 * 3. Jaccard similarity for semantic matching
 */
@Slf4j
@Service
public class SemanticCacheService {

    private final KeywordExtractionService keywordExtractionService;
    private final RedisTemplate<String, Object> redisTemplate;

    // L1 in-memory cache for hot entries (Caffeine)
    private Cache<String, SemanticCacheEntry> l1Cache;

    @Value("${semantic.cache.similarity-threshold:0.70}")
    private double similarityThreshold;

    @Value("${semantic.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${semantic.cache.ttl-minutes:60}")
    private long ttlMinutes;

    @Value("${semantic.cache.l1-max-size:1000}")
    private int l1MaxSize;

    // Cache namespace prefixes
    private static final String CACHE_INDEX_KEY = "semantic:index";
    private static final String CACHE_ENTRY_PREFIX = "semantic:entry:";

    // Common stop words for question normalization
    private static final Set<String> QUESTION_STOP_WORDS = Set.of(
            "did", "do", "does", "what", "when", "where", "which", "who", "why", "how",
            "is", "are", "was", "were", "will", "would", "could", "should", "can", "may",
            "i", "you", "we", "they", "he", "she", "it", "my", "your", "our", "their",
            "learn", "learned", "learning", "know", "knew", "knowing", "remember",
            "anything", "something", "nothing", "everything", "thing", "things",
            "related", "about", "regarding", "concerning", "to", "for", "with", "from",
            "a", "an", "the", "and", "or", "but", "have", "has", "had", "any", "some");

    public SemanticCacheService(KeywordExtractionService keywordExtractionService,
            RedisTemplate<String, Object> redisTemplate) {
        this.keywordExtractionService = keywordExtractionService;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // Initialize Caffeine L1 cache
        this.l1Cache = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
        log.info("Initialized L1 Caffeine cache with maxSize={}, ttl={}min", l1MaxSize, ttlMinutes);
    }

    /**
     * Normalize a question into a set of significant keywords.
     */
    public Set<String> normalizeQuestion(String question) {
        if (question == null || question.isBlank()) {
            return Set.of();
        }

        String[] words = question.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");

        Set<String> keywords = Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(w -> !QUESTION_STOP_WORDS.contains(w))
                .collect(Collectors.toCollection(HashSet::new));

        if (keywords.size() < 2) {
            List<String> rakeKeywords = keywordExtractionService.extractKeywords(question, 5);
            for (String kw : rakeKeywords) {
                keywords.addAll(Arrays.asList(kw.toLowerCase().split("\\s+")));
            }
            keywords.removeAll(QUESTION_STOP_WORDS);
        }

        log.debug("Normalized '{}' to keywords: {}", question, keywords);
        return keywords;
    }

    /**
     * Calculate Jaccard similarity between two keyword sets.
     */
    public double calculateSimilarity(Set<String> set1, Set<String> set2) {
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
     * Look up a semantically similar cached answer.
     * Optimized with L1 cache and batch Redis operations.
     */
    public SemanticCacheResult lookup(String cacheName, String question) {
        if (!cacheEnabled) {
            return SemanticCacheResult.miss();
        }

        long startTime = System.currentTimeMillis();
        Set<String> queryKeywords = normalizeQuestion(question);
        if (queryKeywords.isEmpty()) {
            return SemanticCacheResult.miss();
        }

        String indexKey = CACHE_INDEX_KEY + ":" + cacheName;

        try {
            // Step 1: Get entry keys from index
            Set<Object> entryKeyObjs = redisTemplate.opsForSet().members(indexKey);
            if (entryKeyObjs == null || entryKeyObjs.isEmpty()) {
                log.debug("No entries in cache '{}'", cacheName);
                return SemanticCacheResult.miss();
            }

            List<String> entryKeys = entryKeyObjs.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

            // Step 2: Check L1 cache first, collect keys that need Redis lookup
            List<SemanticCacheEntry> entries = new ArrayList<>();
            List<String> keysToFetch = new ArrayList<>();

            for (String key : entryKeys) {
                SemanticCacheEntry l1Entry = l1Cache.getIfPresent(key);
                if (l1Entry != null) {
                    entries.add(l1Entry);
                } else {
                    keysToFetch.add(key);
                }
            }

            // Step 3: Batch fetch missing entries from Redis (single network call!)
            if (!keysToFetch.isEmpty()) {
                List<Object> redisResults = redisTemplate.opsForValue().multiGet(keysToFetch);
                if (redisResults != null) {
                    for (int i = 0; i < keysToFetch.size(); i++) {
                        Object result = redisResults.get(i);
                        if (result instanceof SemanticCacheEntry entry) {
                            entries.add(entry);
                            // Populate L1 cache
                            l1Cache.put(keysToFetch.get(i), entry);
                        }
                    }
                }
            }

            // Step 4: Find best match using Jaccard similarity
            SemanticCacheEntry bestMatch = null;
            double bestSimilarity = 0.0;

            for (SemanticCacheEntry entry : entries) {
                double similarity = calculateSimilarity(queryKeywords, entry.getNormalizedKeywords());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = entry;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;

            if (bestMatch != null && bestSimilarity >= similarityThreshold) {
                log.info("Cache HIT in {}ms: similarity={} for '{}'",
                        elapsed, String.format("%.2f", bestSimilarity), question);
                return SemanticCacheResult.hit(bestMatch.getCachedValue(), bestSimilarity,
                        bestMatch.getOriginalQuestion());
            }

            log.debug("Cache MISS in {}ms: best similarity={}", elapsed, String.format("%.2f", bestSimilarity));
            return SemanticCacheResult.miss();

        } catch (Exception e) {
            log.warn("Error during cache lookup: {}", e.getMessage());
            return SemanticCacheResult.miss();
        }
    }

    /**
     * Store an entry in the semantic cache.
     */
    public void store(String cacheName, String question, String value, double relevanceScore) {
        if (!cacheEnabled) {
            return;
        }

        Set<String> keywords = normalizeQuestion(question);
        if (keywords.isEmpty()) {
            return;
        }

        try {
            SemanticCacheEntry entry = new SemanticCacheEntry(question, keywords, value, relevanceScore);
            String entryKey = CACHE_ENTRY_PREFIX + cacheName + ":" +
                    keywords.stream().sorted().collect(Collectors.joining("_"));
            String indexKey = CACHE_INDEX_KEY + ":" + cacheName;

            // Store in Redis with TTL
            redisTemplate.opsForValue().set(entryKey, entry, Duration.ofMinutes(ttlMinutes));
            redisTemplate.opsForSet().add(indexKey, entryKey);

            // Also store in L1 cache
            l1Cache.put(entryKey, entry);

            log.info("Stored in cache '{}': keywords={}", cacheName, keywords);
        } catch (Exception e) {
            log.warn("Error storing in cache: {}", e.getMessage());
        }
    }

    /**
     * Clear all entries from a cache namespace.
     */
    public void clearCache(String cacheName) {
        String indexKey = CACHE_INDEX_KEY + ":" + cacheName;
        try {
            Set<Object> entryKeys = redisTemplate.opsForSet().members(indexKey);
            if (entryKeys != null) {
                for (Object key : entryKeys) {
                    String keyStr = key.toString();
                    redisTemplate.delete(keyStr);
                    l1Cache.invalidate(keyStr);
                }
            }
            redisTemplate.delete(indexKey);
            log.info("Cleared cache '{}'", cacheName);
        } catch (Exception e) {
            log.warn("Error clearing cache: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getStats(String cacheName) {
        String indexKey = CACHE_INDEX_KEY + ":" + cacheName;
        Map<String, Object> stats = new HashMap<>();

        try {
            Long redisSize = redisTemplate.opsForSet().size(indexKey);
            stats.put("redisEntryCount", redisSize != null ? redisSize : 0);
            stats.put("l1CacheSize", l1Cache.estimatedSize());
            stats.put("l1HitRate", l1Cache.stats().hitRate());
            stats.put("enabled", cacheEnabled);
            stats.put("similarityThreshold", similarityThreshold);
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }

        return stats;
    }
}
