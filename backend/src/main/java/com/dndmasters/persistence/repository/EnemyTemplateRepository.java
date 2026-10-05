package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.EnemyTemplateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnemyTemplateRepository extends JpaRepository<EnemyTemplateEntity, Long> {

    List<EnemyTemplateEntity> findByCampaignId(Long campaignId);

    List<EnemyTemplateEntity> findByCampaignIdAndName(Long campaignId, String name);
}
