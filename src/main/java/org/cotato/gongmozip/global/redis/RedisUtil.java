package org.cotato.gongmozip.global.redis;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// Redis 범용 유틸리티 클래스 - 비즈니스 로직(prefix 조합, TTL 설정 등)은 Service 계층에서 처리
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    // TTL 포함 저장
    public void set(String key, String value, long timeout, TimeUnit unit) {
        validateInput(key);
        validateInput(value);
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Redis 저장: key={}, TTL={}{}", key, timeout, unit);
    }

    // 조회
    public String get(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    // 원자적으로 조회 후 삭제
    public String getAndDelete(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    // 삭제
    public void delete(String key) {
        validateInput(key);
        redisTemplate.delete(key);
        log.debug("Redis 삭제: key={}", key);
    }

    // 키 존재 여부 확인
    public boolean exists(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 원자적 증가
    public long increment(String key) {
        validateInput(key);
        Long count = redisTemplate.opsForValue().increment(key);
        return count != null ? count : 1L;
    }

    // 원자적 증가 후 최초 생성 시에만 TTL 설정
    public long incrementWithTtl(String key, long timeout, TimeUnit unit) {
        validateInput(key);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, timeout, unit);
        }
        return count != null ? count : 1L;
    }

    // 입력값 검증
    private void validateInput(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(value + "는 null이거나 빈 문자열일 수 없습니다.");
        }
    }
}
