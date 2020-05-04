package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;

public class GeneralServiceImpl implements GeneralService {

    @Autowired
    private KeycloakAdminProperties prop;

    public Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }
}
