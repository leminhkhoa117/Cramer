package com.cramer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ConditionalDataSourceConfig {

    @Value("${spring.datasource.url:}")
    private String dsUrl;

    @Value("${spring.datasource.username:}")
    private String dsUser;

    @Value("${spring.datasource.password:}")
    private String dsPass;

    @Bean
    @ConditionalOnExpression("'${spring.datasource.url:}' != ''")
    public DataSource conditionalDataSource() {
        // This bean will only be created if spring.datasource.url is set and non-empty.
        HikariConfig config = new HikariConfig();
        if (dsUrl == null || dsUrl.isBlank()) {
            throw new IllegalStateException("spring.datasource.url is empty");
        }
        config.setJdbcUrl(dsUrl);
        if (dsUser != null && !dsUser.isEmpty()) config.setUsername(dsUser);
        if (dsPass != null && !dsPass.isEmpty()) config.setPassword(dsPass);
        config.setInitializationFailTimeout(0);
        return new HikariDataSource(config);
    }
}
