package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

@Converter
public class InterestCategoryListConverter implements AttributeConverter<List<InterestCategory>, String> {

    @Override
    public String convertToDatabaseColumn(List<InterestCategory> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<InterestCategory> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .map(InterestCategory::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
