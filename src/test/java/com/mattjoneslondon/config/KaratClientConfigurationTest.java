package com.mattjoneslondon.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.client.HttpGraphQlClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KaratClientConfigurationTest {
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "karat.url=https://test.example.com/graphql",
            "karat.api-key=test-key"
    })
    class WithoutProxy {
        @Autowired
        HttpGraphQlClient karatGraphQlClient;

        @Test
        void clientBeanIsCreated() {
            assertNotNull(karatGraphQlClient);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "karat.url=https://test.example.com/graphql",
            "karat.api-key=test-key",
            "karat.proxy.host=proxy.example.com",
            "karat.proxy.port=8080"
    })
    class WithProxy {
        @Autowired
        HttpGraphQlClient karatGraphQlClient;

        @Test
        void clientBeanIsCreatedWithProxy() {
            assertNotNull(karatGraphQlClient);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "karat.url=https://test.example.com/graphql",
            "karat.api-key=test-key",
            "karat.proxy.host=proxy.example.com",
            "karat.proxy.port=8080",
            "karat.proxy.username=proxyuser",
            "karat.proxy.password=proxypass"
    })
    class WithAuthenticatedProxy {
        @Autowired
        HttpGraphQlClient karatGraphQlClient;

        @Test
        void clientBeanIsCreatedWithAuthenticatedProxy() {
            assertNotNull(karatGraphQlClient);
        }
    }
}
