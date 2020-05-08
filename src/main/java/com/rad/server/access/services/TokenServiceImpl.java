package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import org.apache.commons.codec.binary.Base64;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Key;

public class TokenServiceImpl implements TokenService {

    @Autowired
    private KeycloakAdminProperties prop;


    private Keycloak getKeycloak() {
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }

    public Object refreshTokenMinutes(){
        Keycloak keycloak=getKeycloak();
        keycloak.tokenManager().getAccessToken().setExpiresIn(15*60);
        return keycloak.tokenManager().getAccessToken();
    }

//    public Object refreshTokenHours(long hours){
//        return refreshTokenMinutes(60*hours);
//    }

    public String userNameFromToken(){
        Keycloak keycloak=getKeycloak();
        String jwtToken = keycloak.tokenManager().getAccessToken().getToken();
        System.out.println("------------ Decode JWT ------------");
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];

        System.out.println("~~~~~~~~~ JWT Header ~~~~~~~");
        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        System.out.println("JWT Header : " + header);


        System.out.println("~~~~~~~~~ JWT Body ~~~~~~~");
        String body = new String(base64Url.decode(base64EncodedBody));
        System.out.println("JWT Body : "+body);

        return jwtToken;
    }

}
