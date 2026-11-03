package com.example.api.notification.service;

import com.example.api.common.type.RedisKeys;
import com.example.api.notification.dto.NotificationDto;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

/**
 * 실시간 알림 서비스
 *
 * <p>SSE(Server-Sent Events)와 Redis Pub/Sub을 결합하여 실시간 알림을 제공합니다.
 * 클라이언트가 SSE 구독 시 해당 사용자의 Redis 채널을 구독하며, 이벤트 발행 시 Redis를 통해 모든 서버 인스턴스로 전파됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
  private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

  private final StringRedisTemplate stringRedisTemplate;
  private final RedisMessageListenerContainer listenerContainer;
  private final ObjectMapper objectMapper;

  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
  private final Map<String, MessageListener> listeners = new ConcurrentHashMap<>();

  /**
   * SSE 구독
   *
   * <p>클라이언트와 SSE 연결을 맺고, 해당 사용자의 Redis 채널을 구독합니다.
   * 기존 연결이 있으면 먼저 정리합니다.
   *
   * @param email 구독할 회원 이메일
   * @return SseEmitter
   */
  public SseEmitter subscribe(String email) {
    cleanup(email);

    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    emitters.put(email, emitter);

    try {
      emitter.send(SseEmitter.event().name("connect").data("connected"));
    } catch (IOException e) {
      emitters.remove(email);
      return emitter;
    }

    MessageListener listener = (message, pattern) -> {
      SseEmitter target = emitters.get(email);
      if (target == null) {
        return;
      }
      try {
        target.send(SseEmitter.event()
            .name("notification")
            .data(new String(message.getBody())));
      } catch (IOException e) {
        log.debug("SSE 전송 실패 (연결 종료): {}", email);
        cleanup(email);
      }
    };

    listeners.put(email, listener);
    listenerContainer.addMessageListener(listener,
        new ChannelTopic(RedisKeys.NOTIFICATION_CHANNEL.key(email)));

    emitter.onCompletion(() -> cleanup(email));
    emitter.onTimeout(() -> cleanup(email));
    emitter.onError(e -> cleanup(email));

    log.debug("SSE 구독 등록: {}", email);
    return emitter;
  }

  /**
   * 알림 발행
   *
   * <p>Redis 채널에 알림을 발행합니다. 해당 채널을 구독 중인 모든 서버 인스턴스가 수신합니다.
   *
   * @param email   수신 대상 회원 이메일
   * @param payload 알림 페이로드
   */
  public void publish(String email, NotificationDto.TodoCompleted payload) {
    try {
      String json = objectMapper.writeValueAsString(payload);
      stringRedisTemplate.convertAndSend(RedisKeys.NOTIFICATION_CHANNEL.key(email), json);
    } catch (Exception e) {
      log.error("알림 발행 실패 (email={}): {}", email, e.getMessage());
    }
  }

  private void cleanup(String email) {
    emitters.remove(email);
    MessageListener listener = listeners.remove(email);
    if (listener != null) {
      listenerContainer.removeMessageListener(listener);
    }
  }
}
