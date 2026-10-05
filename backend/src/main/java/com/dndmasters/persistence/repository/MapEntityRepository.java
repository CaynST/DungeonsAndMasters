package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.MapEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapEntityRepository extends JpaRepository<MapEntity, Long> {

    List<MapEntity> findByCampaignId(Long campaignId);

    Optional<MapEntity> findFirstByCampaignId(Long campaignId);
}
