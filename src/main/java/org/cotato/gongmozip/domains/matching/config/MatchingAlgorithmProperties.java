package org.cotato.gongmozip.domains.matching.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 풀 크기 경계와 탐색 횟수·마감시간을 코드에서 분리해 운영 환경별로 조정할 수 있게 만든 설정 객체다.
 * 검증된 기본값을 제공해 잘못된 설정으로 알고리즘이 실행되지 않도록 한다.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "matching.algorithm")
public class MatchingAlgorithmProperties {

    // 이 인원까지는 정확한 결과를 얻기 위해 완전탐색을 우선 사용한다.
    @Min(3)
    private int bruteforceMaxPoolSize = 16;

    // Greedy의 결정적 기본 순서 외에 추가로 생성할 시드 기반 셔플 횟수다.
    @Min(1)
    private int greedyRestartCount = 50;

    // 완전탐색 마감 뒤 Greedy fallback에 보장할 최소 실행시간이다.
    @NotNull
    private Duration fallbackReserve = Duration.ofMinutes(10);

    // 당일 모든 풀의 계산이 끝나야 하는 운영 마감시각이다.
    @NotNull
    private LocalTime batchDeadline = LocalTime.of(15, 30);

    // 계산 결과를 사용자에게 공개하기로 예약한 시각이다.
    @NotNull
    private LocalTime resultPublishTime;
}
