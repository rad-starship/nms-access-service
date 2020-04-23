package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import javax.websocket.OnClose;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RoleServiceImpl implements RoleService {
    @Override
    public List<Role> getKeycloakRoles() {
        List<Role> output = new LinkedList<>();
        Keycloak keycloak = Keycloak.getInstance(
                "http://localhost:8080/auth",// keycloak address
                "master", // ​​specify Realm master
                "avielavitan", // ​​administrator account
                "aviel5002", // ​​administrator password
                "admin-cli");
        RealmResource relamResource = keycloak.realm("Admin");
        RolesResource roles =  relamResource.roles();
        roles.list().forEach(role->output.add(new Role(role.getName())));
        return output;

    }

    @Override
    public void addKeycloakRole(Role role) {
        Keycloak keycloak = Keycloak.getInstance(
                "http://localhost:8080/auth",// keycloak address
                "master", // ​​specify Realm master
                "amirloe", // ​​administrator account
                "aM1rl994", // ​​administrator password
                "admin-cli");
        RealmResource realmResource = keycloak.realm("Admin");
        RoleRepresentation newRole = new RoleRepresentation();
        newRole.setName(role.getName());
        realmResource.roles().create(newRole);
    }

}
