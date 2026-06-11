package com.cramer.platform.config;

import com.cramer.platform.integration.llm.LlmProperties;
import com.cramer.platform.integration.openrouter.OpenRouterProperties;
import com.cramer.platform.integration.supabase.SupabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs a one-line configuration summary at startup and warns on missing critical settings
 * (SPEC-18 §8). Secrets are never logged — only presence flags. The JWT secret itself is
 * validated fail-fast in {@code SupabaseJwtConfig}.
 */
@Component
public class StartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final SupabaseProperties supabase;
    private final LlmProperties llm;
    private final OpenRouterProperties openRouter;
    private final String datasourceUrl;

    public StartupValidator(SupabaseProperties supabase,
                            LlmProperties llm,
                            OpenRouterProperties openRouter,
                            @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.supabase = supabase;
        this.llm = llm;
        this.openRouter = openRouter;
        this.datasourceUrl = datasourceUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!present(datasourceUrl)) {
            log.warn("spring.datasource.url is not set — database access will fail.");
        }
        log.info("Cramer startup config: db={}, supabaseAdmin={}, deepSeek={}, openRouter={}",
                present(datasourceUrl),
                present(supabase.serviceRoleKey()),
                llm.hasApiKey(),
                openRouter.hasApiKey());
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
