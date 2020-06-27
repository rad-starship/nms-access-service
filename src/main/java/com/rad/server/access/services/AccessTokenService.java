package com.rad.server.access.services;

//using example from https://gist.github.com/thomasdarimont/52152ed68486c65b50a04fcf7bd9bbde

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Event;
import com.rad.server.access.entities.LoginEntity;
import com.rad.server.access.entities.User;
import com.rad.server.access.presistance.EsConnectionHandler;
import com.rad.server.access.repositories.UserRepository;
import com.rad.server.access.responses.HttpResponse;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class AccessTokenService {
    private final String clientId = "corona-nms";

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    private AccessToken token;

    @Autowired
    private HashSet<String> tokenBlackList;

    @Autowired
    private UserRepository userRepository;


    /**
     * This function creates request params that will be sent to keycloak server for login.
     * @param loginEntity - an object contains all information for login
     * @return response of KC server for the login function.
     */
    public Object login(LoginEntity loginEntity){
        try {
            MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
            requestParams.add("username", loginEntity.getUsername());
            requestParams.add("password", loginEntity.getPassword());
            requestParams.add("client_id",clientId);
            requestParams.add("grant_type","password");
            if(loginEntity.getOtp()!=null)
                requestParams.add("totp",loginEntity.getOtp());

            return logInUserSession(requestParams,loginEntity.getTenant());

        } catch (Exception e) {
            return new HttpResponse(HttpStatus.BAD_REQUEST,e.getMessage()).getHttpResponse();
        }
    }

    /**
     * This function sends to the KC server login request.
     * @param requestParams - params of the login request
     * @param realm - the realm of the login
     * @return The response of the KC server for the login.
     * @throws Exception - if there is unknown exception it will be thrown.
     */
    private Object logInUserSession(MultiValueMap<String, String> requestParams,String realm) throws Exception {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);

            String url = prop.getServerUrl()+"/realms/" + realm + "/protocol/openid-connect/token";
            return new RestTemplate().postForEntity(url, request, Object.class);
            // got response 204, no content
        }

        catch (HttpClientErrorException e){
            if(e.getStatusCode().value()==401){
                return new HttpResponse(HttpStatus.UNAUTHORIZED,"Invalid user name or password").getHttpResponse();
            }
            else if(e.getStatusCode().value() == 404){
                return new HttpResponse(HttpStatus.NOT_FOUND, "Invalid tenant name").getHttpResponse();
            }
            else if (e.getStatusCode().value()== 400){
                return new HttpResponse(HttpStatus.BAD_REQUEST, e.getMessage()).getHttpResponse();
            }
            else
                throw new Exception("Unknown Error");
        }
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
            return new HttpResponse(HttpStatus.BAD_REQUEST,e.getMessage()).getHttpResponse();
        }
    }

    /**
     * The function extract from JSON string the refresh token
     * @param refreshToken - the Json String
     * @return only refresh token.
     */
    private String parseRefresh(String refreshToken) {
    	if (refreshToken.contains(":"))
    	{
	        String result = refreshToken.split(":")[1];
	        return result.substring(1,result.length()-2);
    	}
    	
    	return refreshToken;
    }

    /**
     * The function send logout request to KC server.
     * @param requestParams - params of the logout request.
     * @return response from KC server.
     */
    private ResponseEntity<?> logoutUserSession(MultiValueMap<String, String> requestParams) {
       try {
           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

           HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);

           String realm = getRealmFromToken();
           String url = prop.getServerUrl()+"/realms/" + getRealmFromToken() + "/protocol/openid-connect/logout";
           return new RestTemplate().postForEntity(url, request, Object.class);
           // got response 204, no content
       }
       catch (HttpClientErrorException e){
           String error = e.getResponseBodyAsString().split(":\"")[2];
           error = error.substring(0,error.indexOf("\""));
           return new HttpResponse(e.getStatusCode(),error).getHttpResponse();
       }
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

    private Keycloak getKeycloak() {
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }

    public boolean isInBlackList(HttpHeaders headers){
        return tokenBlackList.contains(headers.get("Authorization").get(0));
    }

    public void addToBlackList(String token){
        tokenBlackList.add(token);
    }

    public User getUserFromToken(String username){
        for (User user: userRepository.findAll()) {
            if(user.getUserName().toLowerCase().equals(username.toLowerCase()))
                return user;
        }
        return null;
    }

    public Object getSessions() {
        Keycloak keycloak = getKeycloak();
       List<ClientRepresentation> clientRepresentations=keycloak.realm(getRealmFromToken()).clients().findByClientId(clientId);
        ClientRepresentation representation=clientRepresentations.get(0);
        ClientResource resource=keycloak.realm(getRealmFromToken()).clients().get(representation.getId());
        List<UserSessionRepresentation> sessions = resource.getUserSessions(0,1000);
        return sessions;
    }

    public Object getEvents() {
        List<Event> events = new ArrayList<>();
        try {
            EsConnectionHandler.makeConnection();
            events =EsConnectionHandler.loadEventsByTenant(getRealmFromToken());
            EsConnectionHandler.closeConnection();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return events;

    }
}
