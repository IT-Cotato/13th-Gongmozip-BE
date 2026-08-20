package org.cotato.gongmozip.domains.notification.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.cotato.gongmozip.domains.notification.enums.NotificationCategory;
import org.cotato.gongmozip.domains.notification.exception.NotificationException;
import org.cotato.gongmozip.domains.notification.exception.codes.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationConverterTest {

    @DisplayName("category가 null이면 전체 조회를 뜻하는 null을 그대로 반환한다.")
    @Test
    void toNotificationCategoryReturnsNullForNull() {
        assertThat(NotificationConverter.toNotificationCategory(null)).isNull();
    }

    @DisplayName("올바른 category 문자열은 해당 enum으로 변환된다.")
    @Test
    void toNotificationCategoryParsesValidValue() {
        assertThat(NotificationConverter.toNotificationCategory("MATCHING")).isEqualTo(NotificationCategory.MATCHING);
    }

    @DisplayName("잘못된 category 문자열은 NotificationException(INVALID_CATEGORY)을 던진다.")
    @Test
    void toNotificationCategoryThrowsForInvalidValue() {
        assertThatThrownBy(() -> NotificationConverter.toNotificationCategory("FOO"))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", NotificationErrorCode.INVALID_CATEGORY);
    }
}
