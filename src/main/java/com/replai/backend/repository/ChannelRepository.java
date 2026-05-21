package com.replai.backend.repository;

import com.replai.backend.entity.Channel;
import com.replai.backend.entity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByBot_IdAndType(Long botId, ChannelType type);

    Optional<Channel> findByToken(String token);
}

