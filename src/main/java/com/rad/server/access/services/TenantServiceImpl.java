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
    private Settings settings;

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
        PasswordPolicy passwordPolicy=settings.getAuthentication().getPasswordPolicy();
        Token token=settings.getAuthentication().getToken();
        otpPolicy otpPolicy=settings.getAuthentication().getOtpPolicy();
        String password="length("+passwordPolicy.getMinimumLength()+") and forceExpiredPasswordChange("+passwordPolicy.getExpirePassword()+") and digits("+passwordPolicy.getDigits()+") and passwordHistory("+passwordPolicy.getNotRecentlyUsed()+")";
        if(passwordPolicy.isNotUsername())
            password+=" and notUsername(undefined)";
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        realm.setEnabled(true);
        //realm.setPasswordPolicy(password);
        realm.setSsoSessionIdleTimeout(token.getSsoSessionIdle()*60);
        realm.setSsoSessionMaxLifespan(token.getSsoSessionMax()*60);
        realm.setOfflineSessionIdleTimeout(token.getOfflineSessionIdle()*60);
        realm.setAccessTokenLifespan(token.getAccessTokenLifespan()*60);
        realm.setOtpPolicyDigits(otpPolicy.getNumberOfDigits());
        realm.setOtpPolicyLookAheadWindow(otpPolicy.getOptTokenPeriod());
        //realm.setOtpPolicyType(otpPolicy.getOptType());
        keycloak.realms().create(realm);
        addAllClients(tenant.getName());
        if(otpPolicy.isEnabled())
            setOTP(tenant.getName());
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
                Tenant newTenant=new Tenant(r.getRealm());
                repository.save(newTenant);
            }
        }


    }




}
