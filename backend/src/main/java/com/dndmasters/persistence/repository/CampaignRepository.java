package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.CampaignEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<CampaignEntity, Long> {

    List<CampaignEntity> findAllByOrderByIdDesc();

    Page<CampaignEntity> findByStatus(String status, Pageable pageable);
}
