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
    private static final String CACHE_INVERTED_INDEX_PREFIX = "semantic:inverted:";

    // Common stop words for question normalization
    private static final Set<String> QUESTION_STOP_WORDS = Set.of(
            "did", "do", "does", "what", "when", "where", "which", "who", "why", "how",
            "is", "are", "was", "were", "will", "would", "could", "should", "can", "may",
            "i", "you", "we", "they", "he", "she", "it", "my", "your", "our", "their",
            "learn", "learned", "learning", "know", "knew", "knowing", "remember",
            "anything", "something", "nothing", "everything", "thing", "things",
            "related", "about", "regarding", "concerning", "to", "for", "with", "from",
            "a", "an", "the", "and", "or", "but", "have", "has", "had", "any", "some", "in");

    // Tech terms that are short but important
    private static final Set<String> SHORT_TECH_TERMS = Set.of("go", "ai", "ml", "db", "sql", "io", "git", "aws",
            "gcp");

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
                .replaceAll("[^a-zA-Z0-9+#.\\s]", " ") // Preserve +, #, . for C++, C#, .NET
                .split("\\s+");

        Set<String> keywords = Arrays.stream(words)
                .map(String::trim)
                .filter(w -> !w.isEmpty())
                .filter(w -> w.length() > 2 || SHORT_TECH_TERMS.contains(w))
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
     * Optimized with Inverted Index (O(K)) and L1 cache.
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

        try {
            // Step 1: Use Inverted Index to find candidate entry keys
            // We construct the list of set keys to union:
            // semantic:inverted:{cacheName}:{keyword}
            List<String> invertedIndexKeys = queryKeywords.stream()
                    .map(kw -> CACHE_INVERTED_INDEX_PREFIX + cacheName + ":" + kw)
                    .collect(Collectors.toList());

            if (invertedIndexKeys.isEmpty()) {
                return SemanticCacheResult.miss();
            }

            // Union distinct parts to get all potentially relevant entry keys
            Set<Object> candidateKeyObjs = redisTemplate.opsForSet().union(invertedIndexKeys);

            if (candidateKeyObjs == null || candidateKeyObjs.isEmpty()) {
                log.debug("No candidates found in inverted index for keywords: {}", queryKeywords);
                return SemanticCacheResult.miss();
            }

            List<String> candidateKeys = candidateKeyObjs.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

            // Limit candidates to avoid potential explosion (though unlikely with natural
            // language)
            if (candidateKeys.size() > 500) {
                candidateKeys = candidateKeys.subList(0, 500);
            }

            // Step 2: Check L1 cache first, collect keys that need Redis lookup
            List<SemanticCacheEntry> entries = new ArrayList<>();
            List<String> keysToFetch = new ArrayList<>();

            for (String key : candidateKeys) {
                SemanticCacheEntry l1Entry = l1Cache.getIfPresent(key);
                if (l1Entry != null) {
                    entries.add(l1Entry);
                } else {
                    keysToFetch.add(key);
                }
            }

            // Step 3: Batch fetch missing entries from Redis
            if (!keysToFetch.isEmpty()) {
                List<Object> redisResults = redisTemplate.opsForValue().multiGet(keysToFetch);
                if (redisResults != null) {
                    for (int i = 0; i < keysToFetch.size(); i++) {
                        Object result = redisResults.get(i);
                        String entryKey = keysToFetch.get(i);
                        if (result instanceof SemanticCacheEntry entry) {
                            entries.add(entry);
                            // Populate L1 cache
                            l1Cache.put(entryKey, entry);
                        } else if (result == null) {
                            // FIX: Lazy Cleanup for Inverted Index Drift
                            // Entry is gone from main store (TTL expired), but still in inverted index.
                            // We schedule it for removal from inverted index sets for THIS query's
                            // keywords.
                            cleanupStaleInvertedIndexEntriesAsync(cacheName, entryKey);
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

            log.debug("Cache MISS in {}ms: best similarity={} (searched {} candidates)",
                    elapsed, String.format("%.2f", bestSimilarity), candidateKeys.size());
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
            // Construct a consistent key based on the sorted keywords to avoid duplicates
            // for same intent
            String entryKey = CACHE_ENTRY_PREFIX + cacheName + ":" +
                    keywords.stream().sorted().collect(Collectors.joining("_"));
            String indexKey = CACHE_INDEX_KEY + ":" + cacheName;

            Duration ttl = Duration.ofMinutes(ttlMinutes);

            // 1. Store the actual entry
            redisTemplate.opsForValue().set(entryKey, entry, ttl);

            // 2. Add to global index (kept for admin/cleanup mostly)
            redisTemplate.opsForSet().add(indexKey, entryKey);
            redisTemplate.expire(indexKey, ttl); // Refresh TTL on the index itself

            // 3. Update Inverted Indices for EACH keyword
            for (String keyword : keywords) {
                String invertedKey = CACHE_INVERTED_INDEX_PREFIX + cacheName + ":" + keyword;
                redisTemplate.opsForSet().add(invertedKey, entryKey);
                // Set TTL on the inverted index key as well (slide it forward)
                redisTemplate.expire(invertedKey, ttl);
            }

            // 4. Also store in L1 cache
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

            // Also attempt to clean up inverted indexes using keys scanning (admin op)
            // Note: In a massive production cluster, we might rely on TTLs instead
            // FIX: Safer bulk delete using SCAN instead of KEYS
            try {
                Set<String> invertedKeys = scanKeys(CACHE_INVERTED_INDEX_PREFIX + cacheName + ":*");
                if (invertedKeys != null && !invertedKeys.isEmpty()) {
                    redisTemplate.delete(invertedKeys);
                }
            } catch (Exception e) {
                log.warn("Could not bulk delete inverted keys, relying on TTL: {}", e.getMessage());
            }

            log.info("Cleared cache '{}'", cacheName);

        } catch (Exception e) {
            log.warn("Error clearing cache: {}", e.getMessage());
        }
    }

    /**
     * Remove a stale entry from inverted indices.
     */
    private void cleanupStaleInvertedIndexEntriesAsync(String cacheName, String entryKey) {
        log.debug("Cleaning up stale entry {} from inverted index for cache '{}'", entryKey, cacheName);
        // The entry key format is: semantic:entry:{cacheName}:{keyword1}_{keyword2}_...
        String prefix = CACHE_ENTRY_PREFIX + cacheName + ":";
        if (entryKey.startsWith(prefix)) {
            String keywordsPart = entryKey.substring(prefix.length());
            String[] keywordsArray = keywordsPart.split("_");
            for (String kw : keywordsArray) {
                String invertedKey = CACHE_INVERTED_INDEX_PREFIX + cacheName + ":" + kw;
                redisTemplate.opsForSet().remove(invertedKey, entryKey);
            }
        }
    }

    /**
     * Safely scan for keys matching a pattern.
     */
    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.keyCommands().scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            } catch (Exception e) {
                log.error("Error scanning keys: {}", e.getMessage());
            }
            return null;
        });
        return keys;
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
