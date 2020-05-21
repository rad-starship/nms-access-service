package com.rad.server.access.Configuration;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;

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
        String realm = request.getHeader("realm");
        if(realm==null){
            realm = "admin";//defualtive value for none value (login request)
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

}
