package com.rad.server.access.services;

import org.keycloak.admin.client.Keycloak;

public interface GeneralService {
    Keycloak getKeycloakInstance();
}
