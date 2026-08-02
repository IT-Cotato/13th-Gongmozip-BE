package org.cotato.gongmozip.domains.matching.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.SectionResponse;

@Converter
public class SectionListConverter implements AttributeConverter<List<SectionResponse>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<SectionResponse> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("List<SectionResponse> 직렬화 실패", e);
        }
    }

    @Override
    public List<SectionResponse> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<ArrayList<SectionResponse>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("List<SectionResponse> 역직렬화 실패", e);
        }
    }
}
