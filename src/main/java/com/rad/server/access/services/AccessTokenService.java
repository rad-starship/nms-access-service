package com.rad.server.access.services;

//using example from https://gist.github.com/thomasdarimont/52152ed68486c65b50a04fcf7bd9bbde

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.LoginEntity;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.BadRequestException;

@Service
public class AccessTokenService {
    private final String clientId = "corona-nms";

    @Autowired
    private KeycloakAdminProperties prop;

    /**
     * This function access to keycloak server to get Token from login details.
     * @param loginEntity - an object contains all information for loin
     * @return On success returns the token, null on failure.
     */
    public AccessTokenResponse getAccessToken(LoginEntity loginEntity){
        try{
        Keycloak keycloak  = Keycloak.getInstance(
                prop.getServerUrl(),
                loginEntity.getTenant(),
                loginEntity.getUsername(),
                loginEntity.getPassword(),
                clientId
        );
        return keycloak.tokenManager().getAccessToken();
        }
        catch (BadRequestException e){
            System.out.println(e.getResponse());
        }
        return null;
    }
}
