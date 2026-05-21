package com.replai.backend.repository;

import com.replai.backend.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    Optional<Chat> findByExternalChatId(String externalChatId);
    Optional<Chat> findByBot_IdAndExternalChatId(Long botId, String externalChatId);
    long countByBot_Id(Long botId);
}
