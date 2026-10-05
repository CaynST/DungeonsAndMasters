package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.CombatStateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombatStateRepository extends JpaRepository<CombatStateEntity, Long> {

    Optional<CombatStateEntity> findFirstByCampaignIdAndActiveTrue(Long campaignId);

    Optional<CombatStateEntity> findFirstByCampaignIdOrderByIdDesc(Long campaignId);
}
