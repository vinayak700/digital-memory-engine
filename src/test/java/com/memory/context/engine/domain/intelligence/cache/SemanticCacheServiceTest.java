package com.memory.context.engine.domain.intelligence.cache;

import com.memory.context.engine.domain.intelligence.KeywordExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private KeywordExtractionService keywordExtractionService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private SetOperations<String, Object> setOps;

    private SemanticCacheService semanticCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        semanticCacheService = new SemanticCacheService(keywordExtractionService, redisTemplate);

        // Use reflection or constructor injection for properties if needed
        // But here we rely on default values or would need to set them via reflection
        // since they are @Value
        // For unit testing without Spring Context, we can use reflection to set private
        // fields
        try {
            var similarityThresholdField = SemanticCacheService.class.getDeclaredField("similarityThreshold");
            similarityThresholdField.setAccessible(true);
            similarityThresholdField.set(semanticCacheService, 0.70);

            var cacheEnabledField = SemanticCacheService.class.getDeclaredField("cacheEnabled");
            cacheEnabledField.setAccessible(true);
            cacheEnabledField.set(semanticCacheService, true);

            var ttlMinutesField = SemanticCacheService.class.getDeclaredField("ttlMinutes");
            ttlMinutesField.setAccessible(true);
            ttlMinutesField.set(semanticCacheService, 60L);

            var l1MaxSizeField = SemanticCacheService.class.getDeclaredField("l1MaxSize");
            l1MaxSizeField.setAccessible(true);
            l1MaxSizeField.set(semanticCacheService, 100);

            semanticCacheService.init();
        } catch (Exception e) {
            fail("Failed to setup test instance fields: " + e.getMessage());
        }
    }

    @Test
    void testNormalizeQuestion() {
        String question = "What is the capital of France?";
        Set<String> keywords = semanticCacheService.normalizeQuestion(question);

        assertTrue(keywords.contains("capital"));
        assertTrue(keywords.contains("france"));
        assertFalse(keywords.contains("what")); // Stop word
    }

    @Test
    void testStorePopulatesInvertedIndex() {
        String question = "apple pie";
        String value = "Apple pie is delicious.";
        Set<String> keywords = Set.of("apple", "pie");

        semanticCacheService.store("test-cache", question, value, 1.0);

        // Verify stored in value ops
        verify(valueOps).set(contains("semantic:entry:test-cache:"), any(SemanticCacheEntry.class),
                any(Duration.class));

        // Verify inverted index updates
        verify(setOps).add(eq("semantic:inverted:test-cache:apple"), contains("semantic:entry:test-cache:"));
        verify(setOps).add(eq("semantic:inverted:test-cache:pie"), contains("semantic:entry:test-cache:"));
    }

    @Test
    void testLookupUsingInvertedIndex() {
        String question = "apple pie";
        String cacheName = "test-cache";

        // Redis Setup
        // Mock finding the candidate keys
        when(setOps.union(anyList())).thenReturn(Set.of("semantic:entry:test-cache:apple_pie"));

        // Mock fetching the entry
        SemanticCacheEntry mockEntry = new SemanticCacheEntry("apple pie is good", Set.of("apple", "pie"), "The Answer",
                1.0);
        when(valueOps.multiGet(anyList())).thenReturn(List.of(mockEntry));

        // Act
        SemanticCacheResult result = semanticCacheService.lookup(cacheName, question);

        // Assert
        assertTrue(result.isHit());
        assertEquals("The Answer", result.getCachedValue());

        // Verify we checked the inverted index
        verify(setOps).union(argThat(list -> {
            List<String> keys = (List<String>) list;
            return keys.stream().anyMatch(k -> k.contains("apple")) &&
                    keys.stream().anyMatch(k -> k.contains("pie"));
        }));
    }

    @Test
    void testLookupMissWhenInvertedIndexEmpty() {
        String question = "unknown topic";
        when(setOps.union(anyList())).thenReturn(Collections.emptySet());

        SemanticCacheResult result = semanticCacheService.lookup("test-cache", question);

        assertFalse(result.isHit());
    }
}
