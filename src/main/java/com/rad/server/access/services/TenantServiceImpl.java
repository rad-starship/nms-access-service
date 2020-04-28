package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
