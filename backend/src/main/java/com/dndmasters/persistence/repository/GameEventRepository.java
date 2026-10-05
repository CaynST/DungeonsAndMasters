package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.GameEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameEventRepository extends JpaRepository<GameEventEntity, Long> {

    List<GameEventEntity> findTop40ByCampaignIdOrderBySequenceDesc(Long campaignId);

    @Query("select coalesce(max(e.sequence), 0) from GameEventEntity e where e.campaignId = :campaignId")
    long maxSequence(@Param("campaignId") Long campaignId);
}
