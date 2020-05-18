package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
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
    private Map<String,ClientRepresentation> clientRepresentationMap = new ConcurrentHashMap<>();
    final String webUri = "http://localhost:4200/*";
    final String serverUri = "http://localhost:8083/*";
    final String dataUri = "http://localhost:8082/*";
    final String nmsUri = "http://localhost:8081/*";


    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }


    @Override
    public void addKeycloakTenant(Tenant tenant) {
        Keycloak keycloak=getKeycloakInstance();
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        realm.setEnabled(true);
        //realm.setPasswordPolicy("length(8) and forceExpiredPasswordChange(365) and notUsername(undefined) and digits(1) and passwordHistory(3)");
        realm.setSsoSessionIdleTimeout(tenant.getSSOSessionIdle()*60);
        realm.setSsoSessionMaxLifespan(tenant.getSSOSessionMax()*60);
        realm.setOfflineSessionIdleTimeout(tenant.getOfflineSessionIdle()*60);
        realm.setAccessTokenLifespan(tenant.getAccessTokenLifespan()*60);
        keycloak.realms().create(realm);
        addAllClients(tenant.getName());
        //setOTP(tenant.getName());
        System.out.println(keycloak.realm("Admin").toRepresentation().getPasswordPolicy());
    }

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

    @Override
    public List<User> getKeycloakTenants() {
        return null;
    }

    @Override
    public void deleteKeycloakTenant(String name) {
        Keycloak keycloak=getKeycloakInstance();
        keycloak.realms().realm(name).remove();
    }

    @Override
    public void updateKeycloakTenant(Tenant tenant, String name) {
        Keycloak keycloak=getKeycloakInstance();
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        keycloak.realms().realm(name).update(realm);
    }

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

    public void initKeycloakTenants(TenantRepository repository){
        Keycloak keycloak=getKeycloakInstance();
        for (Tenant t:repository.findAll()) {
            if(!tenantExists(t))
                addKeycloakTenant(t);
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
                Tenant newTenant=new Tenant(r.getRealm(),r.getSsoSessionIdleTimeout(),r.getSsoSessionMaxLifespan(),r.getOfflineSessionIdleTimeout(),r.getAccessTokenLifespan());
                repository.save(newTenant);
            }
        }


    }




}
