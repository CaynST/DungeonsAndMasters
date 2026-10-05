package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.ItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {

    List<ItemEntity> findByCampaignId(Long campaignId);
}
