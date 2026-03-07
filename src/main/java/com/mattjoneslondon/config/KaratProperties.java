package com.mattjoneslondon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "karat")
public record KaratProperties(String url,
                              String apiKey,
                              Proxy proxy) {
    public record Proxy(String host,
                        int port,
                        String username,
                        String password) {
    }
}
