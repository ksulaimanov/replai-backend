package com.replai.backend.repository;

import com.replai.backend.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    List<Lead> findByBot_IdOrderByCreatedAtDesc(Long botId);
    long countByBot_Id(Long botId);
    boolean existsByBot_IdAndExternalChatId(Long botId, String externalChatId);
}
