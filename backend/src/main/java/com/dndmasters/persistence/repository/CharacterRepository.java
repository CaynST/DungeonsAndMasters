package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.CharacterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {

    List<CharacterEntity> findByPredefinedTrueOrderByIdAsc();

    List<CharacterEntity> findByOwnerIdOrderByIdAsc(Long ownerId);
}
