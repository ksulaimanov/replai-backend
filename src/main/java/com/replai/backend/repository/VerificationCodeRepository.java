package com.replai.backend.repository;

import com.replai.backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByCode(String code);

    Optional<VerificationCode> findByUser_Email(String email);

    Optional<VerificationCode> findByUser_Id(Long userId);
}
