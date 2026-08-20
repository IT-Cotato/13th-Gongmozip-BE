package org.cotato.gongmozip.global.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firebase Admin SDK로 실제 FCM 발송을 담당한다. {@code firebase.credentials-base64}(서비스 계정 JSON을
 * base64로 인코딩한 값)가 비어있으면 {@link #isEnabled()}가 false를 반환하고 발송을 조용히 건너뛴다 —
 * {@code global.ai.AiGatewayClient.isEnabled()}와 동일한 패턴이라, Firebase 프로젝트가 아직 없어도
 * (docs/decisions/13-fcm-push.md) 나머지 코드는 그대로 컴파일·테스트된다.
 */
@Slf4j
@Component
public class FirebaseFcmClient implements FcmClient {

    private final FirebaseMessaging firebaseMessaging;

    public FirebaseFcmClient(@Value("${firebase.credentials-base64:}") String credentialsBase64) {
        this.firebaseMessaging = initMessaging(credentialsBase64);
    }

    private FirebaseMessaging initMessaging(String credentialsBase64) {
        if (credentialsBase64 == null || credentialsBase64.isBlank()) {
            log.info("firebase.credentials-base64가 비어있어 FCM 발송이 비활성화됩니다.");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            FirebaseOptions options =
                    FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app =
                    FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error("Firebase 초기화에 실패해 FCM 발송이 비활성화됩니다 — 자격증명을 확인하세요.", e);
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return firebaseMessaging != null;
    }

    @Override
    public PushSendResult send(String token, PushPayload payload) {
        if (!isEnabled()) {
            return PushSendResult.FAILURE;
        }
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build())
                .putAllData(payload.data())
                .build();
        try {
            firebaseMessaging.send(message);
            return PushSendResult.SUCCESS;
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                return PushSendResult.INVALID_TOKEN;
            }
            log.warn("FCM 발송 실패 - errorCode: {}", e.getMessagingErrorCode(), e);
            return PushSendResult.FAILURE;
        }
    }
}
