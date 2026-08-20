package org.cotato.gongmozip.global.push;

/** {@code global.ai.AiClient}/{@code AiGatewayClient}와 동일한 패턴 — 자격증명이 없으면 {@link #isEnabled()}가 false를 반환한다. */
public interface FcmClient {

    boolean isEnabled();

    PushSendResult send(String token, PushPayload payload);
}
