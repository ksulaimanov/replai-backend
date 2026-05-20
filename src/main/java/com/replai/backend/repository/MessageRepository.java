package com.replai.backend.repository;

import com.replai.backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.bot.id = :botId")
    long countByBotId(@Param("botId") Long botId);
}
