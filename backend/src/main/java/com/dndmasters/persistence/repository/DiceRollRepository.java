package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.DiceRollEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiceRollRepository extends JpaRepository<DiceRollEntity, Long> {

    List<DiceRollEntity> findTop30ByCampaignIdOrderByIdDesc(Long campaignId);
}
