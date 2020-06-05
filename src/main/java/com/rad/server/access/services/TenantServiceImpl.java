package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.entities.settings.PasswordPolicy;
import com.rad.server.access.entities.settings.Settings;
import com.rad.server.access.entities.settings.Token;
import com.rad.server.access.entities.settings.otpPolicy;
import com.rad.server.access.repositories.TenantRepository;
import org.apache.commons.codec.binary.Base64;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantServiceImpl implements TenantService {

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    private  SettingsService settingsService;

    @Autowired
    private Settings settings;

    private Map<String,ClientRepresentation> clientRepresentationMap = new ConcurrentHashMap<>();
    final String webUri = "http://localhost:4200/*";
    final String serverUri = "http://localhost:8083/*";
    final String dataUri = "http://localhost:8082/*";
    final String nmsUri = "http://localhost:8081/*";

    /**
     * This function returns the keycloak server
     * @return
     */
    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }

    /**
     * This function creates a new tenant(realm) in the keycloak server and configures it to match the current settings
     * @param tenant
     */
    @Override
    public void addKeycloakTenant(Tenant tenant) {
        Keycloak keycloak=getKeycloakInstance();
        Token token=settings.getAuthentication().getToken();
        otpPolicy otpPolicy=settings.getAuthentication().getOtpPolicy();
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        realm.setEnabled(true);
        settingsService.applyTokenToRealm(token, realm);
        keycloak.realms().create(realm);
        addAllClients(tenant.getName());
        if(otpPolicy.isEnabled())
            setOTP(tenant.getName());
    }

    @Override
    public void addIdentityProvider(String providerID,String secret,String clientID,String realm){
        Keycloak keycloak=getKeycloakInstance();
        IdentityProviderRepresentation identityProviderRepresentation=new IdentityProviderRepresentation();
        Map<String,String> config=new HashMap<>();
        config.put("clientSecret",secret);
        config.put("clientId",clientID);
        config.put("useJwkUrl","true");
        identityProviderRepresentation.setConfig(config);
        identityProviderRepresentation.setAlias(providerID);
        identityProviderRepresentation.setProviderId(providerID);
        identityProviderRepresentation.setStoreToken(true);
        identityProviderRepresentation.setEnabled(true);
        keycloak.realm(realm).identityProviders().create(identityProviderRepresentation);
    }


    /**
     * This function sets OTP authentication for the keycloak tenant
     * @param realm
     */
    private void setOTP(String realm){
        Keycloak keycloak=getKeycloakInstance();
        List<RequiredActionProviderRepresentation> requiredActionProviderRepresentations;
        requiredActionProviderRepresentations=keycloak.realm(realm).flows().getRequiredActions();
        for(RequiredActionProviderRepresentation r:requiredActionProviderRepresentations){
            if(r.getName().equals("Configure OTP")){
                r.setEnabled(true);
                r.setDefaultAction(true);
                keycloak.realm(realm).flows().updateRequiredAction(r.getAlias(),r);
                break;
            }
        }
    }


    /**
     * This function adds all of the needed clients to the keycloak tenant
     * @param realm
     */
    private void addAllClients(String realm) {
        //CORONA-WEB-CLIENT
        ProtocolMapperRepresentation protocolMapperRepresentation=new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("details");
        protocolMapperRepresentation.setProtocol("openid-connect");
        protocolMapperRepresentation.setProtocolMapper("User Attribute");
        ClientRepresentation web = getClientRep("corona-web",webUri);
        ClientRepresentation server = getClientRep("corona-server",serverUri);
        ClientRepresentation nms = getClientRep("corona-nms",nmsUri);
        ClientRepresentation health = getClientRep("health-data",dataUri);
        ClientsResource clients = getKeycloakInstance().realm(realm).clients();

        try{

            clients.create(web);
            clients.create(server);
            clients.create(nms);
            clients.create(health);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Client already exists");
        }
    }

    private ClientRepresentation getClientRep(String name,String uri){
        if(clientRepresentationMap.get(name)!= null){
            return clientRepresentationMap.get(name);
        }
        else
        {
        ClientRepresentation output = new ClientRepresentation();
        output.setClientId(name);
        List<String> urls = new ArrayList<>();
        urls.add(uri);
        output.setRedirectUris(urls);
        output.setPublicClient(true);
        output.setDirectAccessGrantsEnabled(true);
        clientRepresentationMap.put(name,output);
        return output;}
    }

    /**
     * This function removes the keycloak tenant by name
     * @param name
     */
    @Override
    public void deleteKeycloakTenant(String name) {
        Keycloak keycloak=getKeycloakInstance();
        keycloak.realms().realm(name).remove();
    }

    /**
     * This function updates an existing keycloak tenant to match the new tenant
     * @param tenant The new tenant
     * @param name
     */
    @Override
    public void updateKeycloakTenant(Tenant tenant, String name) {
        Keycloak keycloak=getKeycloakInstance();
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        keycloak.realms().realm(name).update(realm);
    }

    /**
     * This function checks if the tenant exists in the keycloak servers
     * @param tenant
     * @return
     */
    @Override
    public boolean tenantExists(Tenant tenant){
        Keycloak keycloak=getKeycloakInstance();
        RealmsResource realms=keycloak.realms();
        List<RealmRepresentation> existingRealms=realms.findAll();
        for (RealmRepresentation r:existingRealms) {
            if(r.getRealm().equals(tenant.getName()))
                return true;
        }
        return false;
    }

    /**
     * This function initializes the keycloak tenants from repository and the updates the repository if there are
     * tenants in keycloak that doesnt exist in the repository
     * @param repository
     */
    public void initKeycloakTenants(TenantRepository repository){
        Keycloak keycloak=getKeycloakInstance();
        for (Tenant t:repository.findAll()) {
            if(!tenantExists(t)) {
                addKeycloakTenant(t);
                addIdentityProvider("google","9u_zxECI9MtXP2kLFV5wWFPk","81109865632-shsg5672op595bmujcuabia21bjenpri.apps.googleusercontent.com",t.getName());
            }
        }
        for(RealmRepresentation r:keycloak.realms().findAll()){
            if(r.getRealm().equals("master"))
                continue;
            //Add Clients for each  KC relm
            addAllClients(r.getRealm());

            boolean exists=false;
            for(Tenant t:repository.findAll()){
                if(t.getName().equals(r.getRealm()))
                    exists=true;
            }
            if(exists)
                continue;
            else{
                Tenant newTenant=new Tenant(r.getRealm());
                repository.save(newTenant);
            }
        }


    }




}
