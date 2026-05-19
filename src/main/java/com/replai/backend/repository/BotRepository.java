package com.replai.backend.repository;

import com.replai.backend.entity.Bot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
    Optional<Bot> findByOwner_Email(String email);
}
