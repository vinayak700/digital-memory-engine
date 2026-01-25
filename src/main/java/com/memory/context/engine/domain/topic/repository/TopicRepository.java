package com.memory.context.engine.domain.topic.repository;

import com.memory.context.engine.domain.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Topic entity.
 */
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByUserId(String userId);

    Optional<Topic> findByUserIdAndName(String userId, String name);
}
