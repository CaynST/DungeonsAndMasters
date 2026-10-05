package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.SceneEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneRepository extends JpaRepository<SceneEntity, Long> {

    List<SceneEntity> findByCampaignIdOrderByOrderIndexAsc(Long campaignId);

    Optional<SceneEntity> findFirstByCampaignIdOrderByOrderIndexAsc(Long campaignId);

    Optional<SceneEntity> findByCampaignIdAndOrderIndex(Long campaignId, int orderIndex);
}
