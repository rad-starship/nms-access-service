package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.TenantRepository;
import org.apache.commons.codec.binary.Base64;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Service
public class TenantServiceImpl implements TenantService {

    @Autowired
    private KeycloakAdminProperties prop;


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
        realm.setSsoSessionIdleTimeout(tenant.getTokenMinutes()*60);
        realm.setSsoSessionMaxLifespan(tenant.getTokenHours()*60*60);
        realm.setOfflineSessionIdleTimeout(tenant.getTokenDays()*24*60);
        realm.setAccessTokenLifespan(tenant.getAccessTokenTimeout());
        keycloak.realms().create(realm);
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
