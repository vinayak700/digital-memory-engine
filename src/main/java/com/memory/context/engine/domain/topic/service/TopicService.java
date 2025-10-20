package com.memory.context.engine.domain.topic.service;

import com.memory.context.engine.domain.common.exception.ResourceNotFoundException;
import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.topic.entity.Topic;
import com.memory.context.engine.domain.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final MemoryRepository memoryRepository;

    @Transactional
    public Topic createTopic(String name, String description) {
        String userId = getCurrentUser();
        log.info("Creating topic '{}' for user: {}", name, userId);

        return topicRepository.findByUserIdAndName(userId, name)
                .orElseGet(() -> {
                    Topic topic = Topic.builder()
                            .userId(userId)
                            .name(name)
                            .description(description)
                            .createdAt(Instant.now())
                            .build();
                    return topicRepository.save(topic);
                });
    }

    @Transactional(readOnly = true)
    public List<Topic> getTopics() {
        return topicRepository.findByUserId(getCurrentUser());
    }

    @Transactional
    public void tagMemory(Long memoryId, List<String> topicNames) {
        String userId = getCurrentUser();
        log.info("Tagging memory {} with topics: {}", memoryId, topicNames);

        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory not found"));

        if (!memory.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Memory not found"); // Security: hide existence
        }

        // Create or fetch topics
        Set<Topic> topics = topicNames.stream()
                .map(name -> createTopic(name, null))
                .collect(Collectors.toSet());

        // Add to memory
        memory.getTopics().addAll(topics);
        memoryRepository.save(memory);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
