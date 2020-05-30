package com.rad.server.access.Configuration;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.apache.commons.codec.binary.Base64;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//REFERENCE : https://github.com/vimalKeshu/movie-app
public class MultitenantConfigResolver implements KeycloakConfigResolver {

    private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();
    private final String authServerUrl = "http://localhost:8080/auth";
    private final String resource = "corona-nms";

    private static AdapterConfig adapterConfig;

    @Override
    public KeycloakDeployment resolve(OIDCHttpFacade.Request request) {
        String realm = "None";
        String auth = request.getHeader("Authorization");

        if(auth!=null) {
            String jwt = auth.split(" ")[1];
            realm = getRealmFromJWT(jwt);
        }

        KeycloakDeployment deployment = cache.get(realm);
        if (null == deployment) {
            // not found on the simple cache, try to load it from the file system
            AdapterConfig adapterConfig = new AdapterConfig();
            adapterConfig.setRealm(realm);
            adapterConfig.setAuthServerUrl(authServerUrl);
            adapterConfig.setResource(resource);
            deployment = KeycloakDeploymentBuilder.build(adapterConfig);
            cache.put(realm, deployment);
        }

        return deployment;
    }

    public static void setAdapterConfig(AdapterConfig adapterConfig) {
        MultitenantConfigResolver.adapterConfig = adapterConfig;
    }

    /**
     * The function recieves the JWT token from header and parse it to find the realm name.
     * @param jwtToken  Original JWT Token
     * @return realm name
     */
    private String getRealmFromJWT(String jwtToken)  {
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedBody = split_string[1];
        Base64 base64Url = new Base64(true);

        String[] body = new String(base64Url.decode(base64EncodedBody)).split(",");
        for (String row : body){
            if (row.contains("iss")){
                String[] realmAdress = row.split("/");
                String realm = realmAdress[realmAdress.length-1];
                realm = realm.substring(0,realm.length()-1);
                return realm;
            }
        }
        return null;
    }
}
