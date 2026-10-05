package com.dndmasters.persistence.repository;

import com.dndmasters.persistence.entity.BoardEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardEntityRepository extends JpaRepository<BoardEntity, Long> {

    List<BoardEntity> findBySceneIdOrderByIdAsc(Long sceneId);

    void deleteBySceneId(Long sceneId);
}
