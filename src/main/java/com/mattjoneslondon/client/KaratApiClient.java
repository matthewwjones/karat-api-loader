package com.mattjoneslondon.client;

import com.mattjoneslondon.domain.KaratGroup;
import com.mattjoneslondon.domain.KaratUser;
import com.mattjoneslondon.domain.client.UserResponse;
import com.mattjoneslondon.domain.client.UsersPage;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class KaratApiClient {
    private static final int PAGE_SIZE = 100;
    private static final String USERS_QUERY = """
            query GetUsers($first: Int, $after: String) {
                users(first: $first, after: $after) {
                    nodes {
                        id
                        name
                        email
                        phone
                        timeZone
                        clientUserType
                        lastSeen
                        groups {
                            nodes {
                                id
                                name
                            }
                        }
                    }
                    pageInfo {
                        hasNextPage
                        endCursor
                    }
                }
            }
            """;
    private final HttpGraphQlClient graphQlClient;

    public KaratApiClient(HttpGraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }

    public List<KaratUser> fetchAllUsers() {
        List<KaratUser> users = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;
        while (hasNextPage) {
            UsersPage page = fetchPage(cursor);
            page.nodes().stream().map(KaratApiClient::toUser).forEach(users::add);
            hasNextPage = page.pageInfo().hasNextPage();
            cursor = page.pageInfo().endCursor();
        }
        return users;
    }

    private UsersPage fetchPage(String cursor) {
        Map<String, Object> variables;
        if (cursor == null) {
            variables = Map.of("first", PAGE_SIZE);
        } else {
            variables = Map.of("first", PAGE_SIZE, "after", cursor);
        }
        return graphQlClient.document(USERS_QUERY)
                            .variables(variables)
                            .retrieve("users")
                            .toEntity(UsersPage.class)
                            .block();
    }

    private static KaratUser toUser(UserResponse response) {
        List<KaratGroup> groups;
        if (response.groups() == null) {
            groups = List.of();
        } else {
            groups = response.groups().nodes();
        }
        return new KaratUser(response.id(), response.name(), response.email(),
                             response.phone(), response.timeZone(), response.clientUserType(),
                             response.lastSeen(), groups);
    }
}