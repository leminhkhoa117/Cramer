package com.cramer.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated, bounded thread pool for ABTS streaming (SSE) generation tasks.
 *
 * <p>Previously the streaming endpoints used {@code CompletableFuture.runAsync(task)},
 * which dispatches onto the shared {@code ForkJoinPool.commonPool()}. Under
 * concurrent load that pool could be exhausted by long-running generation work,
 * starving the rest of the application (a denial-of-service risk). This bounded
 * pool caps concurrency and rejects overflow back onto the caller thread.
 *
 * @since 2026 - ABTS refactor PART B (B3)
 */
@Configuration
public class AbtsStreamingAsyncConfig {

    @Value("${abts.streaming.executor.core-size:8}")
    private int coreSize;

    @Value("${abts.streaming.executor.max-size:8}")
    private int maxSize;

    @Value("${abts.streaming.executor.queue-capacity:4}")
    private int queueCapacity;

    @Value("${abts.streaming.executor.keep-alive-seconds:120}")
    private int keepAliveSeconds;

    @Bean("abtsStreamingExecutor")
    public TaskExecutor abtsStreamingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("abts-stream-");
        // FIX 1: AbortPolicy instead of CallerRunsPolicy. SSE generation is a
        // long-running blocking task; running it on the request (caller) thread
        // would tie up the Tomcat worker for the full generation duration,
        // re-introducing the very thread-starvation DoS this pool exists to
        // prevent. Reject overflow fast so the controller can emit a clean
        // "server at capacity" SSE event and release the connection.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // Idle threads (including core) are reclaimed after keepAlive so the pool
        // does not pin 8 OS threads permanently when generation is idle.
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
