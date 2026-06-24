package org.cotato.gongmozip.global.swagger;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

// springdoc은 컨텍스트에 등록된 OperationCustomizer 빈을 자동으로 감지해 적용한다. SwaggerConfig에 별도 등록 불필요.
@Component
public class ErrorCodeOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        CustomErrorCodes annotation = handlerMethod.getMethodAnnotation(CustomErrorCodes.class);
        if (annotation == null) {
            return operation;
        }

        List<BaseErrorCode> allErrorCodes = collectErrorCodes(annotation);

        // PostErrorCode처럼 성공/에러 코드가 같은 enum에 섞여 있으므로 4xx/5xx만 Swagger 에러 응답으로 추가
        // LinkedHashMap::new: 상태코드 순서를 삽입 순서대로 유지해 Swagger UI에서 400→404→500 순으로 노출
        Map<Integer, List<BaseErrorCode>> groupedByStatus = allErrorCodes.stream()
                .filter(ec -> ec.getHttpStatus().isError())
                .collect(Collectors.groupingBy(
                        ec -> ec.getHttpStatus().value(), LinkedHashMap::new, Collectors.toList()));

        if (groupedByStatus.isEmpty()) {
            return operation;
        }

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        for (Map.Entry<Integer, List<BaseErrorCode>> entry : groupedByStatus.entrySet()) {
            responses.addApiResponse(entry.getKey().toString(), buildSwaggerResponse(entry.getValue()));
        }

        return operation;
    }

    private List<BaseErrorCode> collectErrorCodes(CustomErrorCodes annotation) {
        List<BaseErrorCode> codes = new ArrayList<>();
        for (Class<?> clazz : annotation.commonErrorCodes()) {
            addEnumConstants(clazz, codes);
        }
        for (Class<?> clazz : annotation.domainErrorCodes()) {
            addEnumConstants(clazz, codes);
        }
        return codes;
    }

    private void addEnumConstants(Class<?> clazz, List<BaseErrorCode> codes) {
        if (!clazz.isEnum()) return;
        for (Object constant : clazz.getEnumConstants()) {
            // ErrorCode를 구현하지 않은 enum이 실수로 넘어와도 무시 (Java 16+ 패턴 매칭)
            if (constant instanceof BaseErrorCode errorCode) {
                codes.add(errorCode);
            }
        }
    }

    private ApiResponse buildSwaggerResponse(List<BaseErrorCode> errorCodes) {
        String description = errorCodes.stream()
                .map(ec -> "- `" + ec.getCode() + "` : " + ec.getMessage())
                .collect(Collectors.joining("\n"));

        return new ApiResponse().description(description);
    }
}
