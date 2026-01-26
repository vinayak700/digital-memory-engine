package com.memory.context.engine.domain.search.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory to retrieve the appropriate SearchStrategy.
 * Uses specific strategy names or defaults to 'vector'.
 */
@Component
public class SearchStrategyFactory {

    private final Map<String, SearchStrategy> strategies;

    public SearchStrategyFactory(List<SearchStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SearchStrategy::getName, Function.identity()));
    }

    public SearchStrategy getStrategy(String name) {
        return Optional.ofNullable(strategies.get(name))
                .orElseThrow(() -> new IllegalArgumentException("Unknown search strategy: " + name));
    }

    public SearchStrategy getDefaultStrategy() {
        return getStrategy("vector");
    }
}
