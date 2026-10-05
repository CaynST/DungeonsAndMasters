package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.CampaignMemberEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignMemberRepository extends JpaRepository<CampaignMemberEntity, Long> {

    List<CampaignMemberEntity> findByCampaignIdOrderByIdAsc(Long campaignId);

    Optional<CampaignMemberEntity> findByCampaignIdAndUserId(Long campaignId, Long userId);

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);

    void deleteByCampaignIdAndUserId(Long campaignId, Long userId);
}
