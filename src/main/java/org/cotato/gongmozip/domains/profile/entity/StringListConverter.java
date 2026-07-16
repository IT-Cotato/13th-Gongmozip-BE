package org.cotato.gongmozip.domains.profile.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("List<String> 직렬화 실패", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmedData = dbData.trim();
        try {
            return objectMapper.readValue(dbData, new TypeReference<ArrayList<String>>() {});
        } catch (JsonProcessingException e) {
            if (looksLikeJson(trimmedData)) {
                throw new IllegalArgumentException("List<String> 역직렬화 실패", e);
            }
            // 기존 콤마 구분 데이터 하위 호환 처리
            List<String> fallback = new ArrayList<>();
            for (String s : dbData.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) fallback.add(trimmed);
            }
            return fallback;
        }
    }

    private boolean looksLikeJson(String value) {
        return value.startsWith("[") || value.startsWith("{") || value.startsWith("\"");
    }
}
