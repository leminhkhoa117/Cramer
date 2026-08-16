package com.cramer.platform.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Application-level caching (Phase 2 follow-up). Two caches, both short TTL so admin edits
 * surface quickly even if an eviction is missed:
 *
 * <ul>
 *   <li>{@code courses} — published test sets, per-course tests, set details</li>
 *   <li>{@code hashtags} — hashtag list/category/popular reads</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_COURSES = "courses";
    public static final String CACHE_HASHTAGS = "hashtags";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CACHE_COURSES, CACHE_HASHTAGS);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1_000));
        return manager;
    }
}
