package com.cramer.abts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bounded executor for ABTS SSE streaming (SPEC-21 §6, SPEC-24 §3). Queue saturation triggers the
 * {@link ThreadPoolExecutor.AbortPolicy} ({@code RejectedExecutionException}) so the streaming
 * service can emit {@code ABORTED} instead of silently dropping a request.
 */
@Configuration
public class AbtsStreamingConfig {

    @Bean(name = "abtsStreamingExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor abtsStreamingExecutor(AbtsProperties props) {
        AbtsProperties.Streaming s = props.streaming();
        return new ThreadPoolExecutor(
                s.poolSize(), s.poolSize(),
                120L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(Math.max(1, s.queueCapacity())),
                r -> {
                    Thread t = new Thread(r, "abts-stream");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }
}
