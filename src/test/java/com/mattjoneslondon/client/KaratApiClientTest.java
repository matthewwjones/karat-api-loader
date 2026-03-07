package com.mattjoneslondon.client;

import com.mattjoneslondon.domain.KaratGroup;
import com.mattjoneslondon.domain.KaratUser;
import com.mattjoneslondon.domain.client.GroupsConnection;
import com.mattjoneslondon.domain.client.PageInfo;
import com.mattjoneslondon.domain.client.UserResponse;
import com.mattjoneslondon.domain.client.UsersPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KaratApiClientTest {
    @Mock
    HttpGraphQlClient graphQlClient;
    @Mock
    GraphQlClient.RequestSpec requestSpec;
    @Mock
    GraphQlClient.RetrieveSpec retrieveSpec;
    KaratApiClient client;

    @BeforeEach
    void setUp() {
        client = new KaratApiClient(graphQlClient);
        when(graphQlClient.document(anyString())).thenReturn(requestSpec);
        when(requestSpec.variables(anyMap())).thenReturn(requestSpec);
        when(requestSpec.retrieve("users")).thenReturn(retrieveSpec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAllUsers_returnsMappedUsersFromSinglePage() {
        var page = new UsersPage(
                List.of(new UserResponse("1", "Alice", "alice@example.com",
                        null, "America/New_York", "ADMIN", null, null)),
                new PageInfo(false, null));
        when(retrieveSpec.toEntity(any(Class.class))).thenReturn(Mono.just(page));

        List<KaratUser> users = client.fetchAllUsers();

        assertAll(
                () -> assertThat(users, hasSize(1)),
                () -> assertThat(users.getFirst().id(), is("1")),
                () -> assertThat(users.getFirst().name(), is("Alice")),
                () -> assertThat(users.getFirst().email(), is("alice@example.com")),
                () -> assertThat(users.getFirst().clientUserType(), is("ADMIN")),
                () -> assertThat(users.getFirst().groups(), empty())
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAllUsers_paginatesUntilHasNextPageIsFalse() {
        var page1 = new UsersPage(
                List.of(new UserResponse("1", "Alice", "alice@example.com",
                        null, null, "ADMIN", null, null)),
                new PageInfo(true, "cursor-abc"));
        var page2 = new UsersPage(
                List.of(new UserResponse("2", "Bob", "bob@example.com",
                        null, null, "TEAM_MEMBER", null, null)),
                new PageInfo(false, null));
        when(retrieveSpec.toEntity(any(Class.class)))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2));

        List<KaratUser> users = client.fetchAllUsers();

        assertAll(
                () -> assertThat(users, hasSize(2)),
                () -> assertThat(users.getFirst().name(), is("Alice")),
                () -> assertThat(users.get(1).name(), is("Bob"))
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchAllUsers_mapsGroupsFromNestedConnection() {
        var groups = new GroupsConnection(
                List.of(new KaratGroup("g1", "Engineering")));
        var page = new UsersPage(
                List.of(new UserResponse("1", "Alice", "alice@example.com",
                        null, null, "ADMIN", null, groups)),
                new PageInfo(false, null));
        when(retrieveSpec.toEntity(any(Class.class))).thenReturn(Mono.just(page));

        List<KaratUser> users = client.fetchAllUsers();

        assertAll(
                () -> assertThat(users.getFirst().groups(), hasSize(1)),
                () -> assertThat(users.getFirst().groups().getFirst().name(), is("Engineering"))
        );
    }
}