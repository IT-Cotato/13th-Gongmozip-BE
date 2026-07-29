package org.cotato.gongmozip.domains.character.repository;

import java.util.List;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinition;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionFeature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterDefinitionFeatureRepository extends JpaRepository<CharacterDefinitionFeature, Long> {

    List<CharacterDefinitionFeature> findAllByDefinitionOrderByDisplayOrderAsc(CharacterDefinition definition);
}
