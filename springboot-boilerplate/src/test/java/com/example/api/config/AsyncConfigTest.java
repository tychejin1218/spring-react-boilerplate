package com.example.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * MDC 전파 TaskDecorator 테스트
 */
class AsyncConfigTest {

  private final TaskDecorator decorator = new AsyncConfig().mdcTaskDecorator();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @AfterEach
  void tearDown() {
    MDC.clear();
    executor.shutdownNow();
  }

  @Test
  @DisplayName("작업을 맡긴 스레드의 MDC 가 비동기 스레드로 복사되고, 작업 후에는 남지 않는다")
  void copiesMdcToWorkerThread() throws Exception {
    MDC.put("traceId", "abc123");
    MDC.put("memberId", "7");
    CompletableFuture<String> inside = new CompletableFuture<>();
    Runnable task = decorator.decorate(
        () -> inside.complete(MDC.get("traceId") + "/" + MDC.get("memberId")));

    executor.submit(task).get(5, TimeUnit.SECONDS);
    String afterTask = executor.submit(() -> MDC.get("traceId")).get(5, TimeUnit.SECONDS);

    assertThat(inside.get()).isEqualTo("abc123/7");
    assertThat(afterTask).isNull();
  }
}
