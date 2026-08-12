package com.centerton.centerton.domain.aichat.repository;

import com.centerton.centerton.domain.aichat.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
}
