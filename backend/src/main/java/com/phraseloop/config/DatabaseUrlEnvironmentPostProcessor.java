package com.phraseloop.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render の DATABASE_URL（postgres://）を JDBC の接続情報へ変換する。
 * SPRING_DATASOURCE_URL が明示されていればそちらを優先する。
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicit = System.getenv("SPRING_DATASOURCE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return;
        }
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        String normalized = databaseUrl;
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }
        URI uri = URI.create(normalized);
        String userInfo = uri.getUserInfo();
        if (userInfo == null || uri.getHost() == null) {
            return;
        }
        int separator = userInfo.indexOf(':');
        String username = decode(separator >= 0 ? userInfo.substring(0, separator) : userInfo);
        String password = separator >= 0 ? decode(userInfo.substring(separator + 1)) : "";
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            jdbcUrl = jdbcUrl + "?" + uri.getRawQuery();
        }
        Map<String, Object> values = new HashMap<>();
        values.put("spring.datasource.url", jdbcUrl);
        values.put("spring.datasource.username", username);
        values.put("spring.datasource.password", password);
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", values));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
