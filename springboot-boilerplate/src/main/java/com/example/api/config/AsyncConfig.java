package com.example.api.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 *
 * <p>이메일 발송 등 I/O 블로킹 작업을 별도 스레드에서 실행하기 위해 {@code @Async}를 활성화합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * MDC 전파용 TaskDecorator
   *
   * <p>{@code @Async} 작업은 다른 스레드에서 실행되어 요청 스레드의 MDC(traceId)를 잃는다.
   * Spring Boot 는 이 빈을 기본 비동기 실행기에 자동으로 적용하므로,
   * 비동기 작업의 로그에도 원래 요청의 traceId 가 남는다.
   *
   * @return MDC 를 복사해 실행하는 TaskDecorator
   */
  @Bean
  public TaskDecorator mdcTaskDecorator() {
    return runnable -> {
      Map<String, String> context = MDC.getCopyOfContextMap();
      return () -> {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
          if (context != null) {
            MDC.setContextMap(context);
          } else {
            MDC.clear();
          }
          runnable.run();
        } finally {
          if (previous != null) {
            MDC.setContextMap(previous);
          } else {
            MDC.clear();
          }
        }
      };
    };
  }
}
