package com.mattjoneslondon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
public class KaratClientConfiguration {
    @Bean
    public HttpGraphQlClient karatGraphQlClient(KaratProperties properties) {
        HttpClient httpClient = buildHttpClient(properties.proxy());
        WebClient webClient = WebClient.builder()
                                       .clientConnector(new ReactorClientHttpConnector(httpClient))
                                       .baseUrl(properties.url())
                                       .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                                       .build();
        return HttpGraphQlClient.builder(webClient).build();
    }

    private HttpClient buildHttpClient(KaratProperties.Proxy proxy) {
        HttpClient httpClient = HttpClient.create();
        if (proxy == null || proxy.host() == null || proxy.host().isBlank()) {
            return httpClient;
        }
        return httpClient.proxy(spec -> configureProxy(spec, proxy));
    }

    private void configureProxy(ProxyProvider.TypeSpec spec, KaratProperties.Proxy proxy) {
        var builder = spec.type(ProxyProvider.Proxy.HTTP)
                          .host(proxy.host())
                          .port(proxy.port());
        if (hasCredentials(proxy)) {
            builder.username(proxy.username())
                   .password(u -> proxy.password());
        }
    }

    private boolean hasCredentials(KaratProperties.Proxy proxy) {
        return proxy.username() != null && !proxy.username().isBlank();
    }
}
