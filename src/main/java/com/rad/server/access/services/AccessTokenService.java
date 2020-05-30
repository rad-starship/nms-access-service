package com.rad.server.access.services;

//using example from https://gist.github.com/thomasdarimont/52152ed68486c65b50a04fcf7bd9bbde

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.LoginEntity;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.BadRequestException;

@Service
public class AccessTokenService {
    private final String clientId = "corona-nms";

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    private AccessToken token;

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

    /**
     * The function creates request params that will be sent to Keycloak server.
     * @param refreshToken - string represents JSON that contains refreshToken
     * @return response from KC server.
     */
    public Object logout(String refreshToken) {
        try {

            MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
            requestParams.add("client_id", clientId);
            requestParams.add("refresh_token", parseRefresh(refreshToken));

            return logoutUserSession(requestParams);

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    /**
     * The function extract from JSON string the refresh token
     * @param refreshToken - the Json String
     * @return only refresh token.
     */
    private String parseRefresh(String refreshToken) {
        String result = refreshToken.split(":")[1];
        return result.substring(1,result.length()-2);
    }

    /**
     * The function send logout request to KC server.
     * @param requestParams - params of the logout request.
     * @return response from KC server.
     */
    private ResponseEntity<Object> logoutUserSession(MultiValueMap<String, String> requestParams) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);

        String realm = getRealmFromToken();
        String url = "http://localhost:8080/auth/realms/"+getRealmFromToken()+"/protocol/openid-connect/logout";

        return new RestTemplate().postForEntity(url, request, Object.class);
        // got response 204, no content
    }

    /**
     * This function get realm out of the access token from issuer field
     * @return realm name.
     */
    private String getRealmFromToken() {
        String[] headers = token.getIssuer().split("/");
        String realm = headers[headers.length - 1];
        return realm;

    }
}
