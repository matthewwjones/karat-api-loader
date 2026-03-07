package com.mattjoneslondon.domain;

import java.util.List;

public record KaratUser(String id,
                        String name,
                        String email,
                        String phone,
                        String timeZone,
                        String clientUserType,
                        String lastSeen,
                        List<KaratGroup> groups) {}