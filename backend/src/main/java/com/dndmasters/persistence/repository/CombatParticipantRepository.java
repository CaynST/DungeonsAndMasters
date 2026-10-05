package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.CombatParticipantEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombatParticipantRepository extends JpaRepository<CombatParticipantEntity, Long> {

    List<CombatParticipantEntity> findByCombatIdOrderByOrderIndexAsc(Long combatId);

    void deleteByCombatId(Long combatId);
}
