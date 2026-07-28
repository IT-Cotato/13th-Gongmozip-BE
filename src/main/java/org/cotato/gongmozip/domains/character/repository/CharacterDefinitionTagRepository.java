package org.cotato.gongmozip.domains.character.repository;

import java.util.List;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinition;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterDefinitionTagRepository extends JpaRepository<CharacterDefinitionTag, Long> {

    List<CharacterDefinitionTag> findAllByDefinitionOrderByDisplayOrderAsc(CharacterDefinition definition);
}
