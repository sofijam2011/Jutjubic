package com.example.jutjubic.repository;

import com.example.jutjubic.model.WatchPartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchPartyMemberRepository extends JpaRepository<WatchPartyMember, Long> {

    List<WatchPartyMember> findByWatchPartyId(Long watchPartyId);

    Optional<WatchPartyMember> findByWatchPartyIdAndUserId(Long watchPartyId, Long userId);

    boolean existsByWatchPartyIdAndUserId(Long watchPartyId, Long userId);

    void deleteByWatchPartyIdAndUserId(Long watchPartyId, Long userId);

    long countByWatchPartyId(Long watchPartyId);
}
