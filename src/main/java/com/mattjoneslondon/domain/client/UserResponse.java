package com.mattjoneslondon.domain.client;

public record UserResponse(String id,
                           String name,
                           String email,
                           String phone,
                           String timeZone,
                           String clientUserType,
                           String lastSeen,
                           GroupsConnection groups) {}