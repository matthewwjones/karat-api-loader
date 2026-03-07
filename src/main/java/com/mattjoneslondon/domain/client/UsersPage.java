package com.mattjoneslondon.domain.client;

import java.util.List;

public record UsersPage(List<UserResponse> nodes, PageInfo pageInfo) {}