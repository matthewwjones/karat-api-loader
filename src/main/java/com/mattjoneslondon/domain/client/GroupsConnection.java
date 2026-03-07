package com.mattjoneslondon.domain.client;

import com.mattjoneslondon.domain.KaratGroup;

import java.util.List;

public record GroupsConnection(List<KaratGroup> nodes) {}