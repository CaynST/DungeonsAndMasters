package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.NpcTemplateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NpcTemplateRepository extends JpaRepository<NpcTemplateEntity, Long> {

    List<NpcTemplateEntity> findByCampaignId(Long campaignId);
}
