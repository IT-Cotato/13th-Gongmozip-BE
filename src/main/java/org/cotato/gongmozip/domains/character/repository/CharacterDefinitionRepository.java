package org.cotato.gongmozip.domains.character.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinition;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterDefinitionRepository extends JpaRepository<CharacterDefinition, Long> {

    Optional<CharacterDefinition> findByCharacterType(CharacterType characterType);
}
