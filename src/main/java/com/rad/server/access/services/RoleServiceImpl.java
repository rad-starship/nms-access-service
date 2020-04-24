package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.repositories.RoleRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.websocket.OnClose;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RoleServiceImpl implements RoleService {

    private Keycloak keycloak = Keycloak.getInstance(
            "http://localhost:8080/auth",// keycloak address
            "master", // ​​specify Realm master
            "amirloe", // ​​administrator account
            "aM1rl994", // ​​administrator password
            "admin-cli");

    @Autowired
    private RoleRepository roleRepository;


    public void getKeycloakRoles() {
        RealmResource relamResource = keycloak.realm("Admin");
        RolesResource roles =  relamResource.roles();
        roles.list().forEach(role->
        {
            Role newRole = new Role(role.getId(), role.getName());
            if(!haveInRepo(newRole))
                roleRepository.save(newRole);
        });
    }

    private boolean haveInRepo(Role newRole) {
        List<Role> repo = (List<Role>) roleRepository.findAll();
        for(Role r : repo){
            if (r.getName().equals(newRole.getName()))
                    return true;
        }
        return false;
    }


    public void addKeycloakRole(Role role) {

        RealmResource realmResource = keycloak.realm("Admin");
        RoleRepresentation newRole = new RoleRepresentation();
        newRole.setName(role.getName());

        realmResource.roles().create(newRole);
    }

    private void deleteKeycloakRole(Role role) {
        RealmResource realmResource = keycloak.realm("Admin");
            realmResource.roles().deleteRole(role.getName());
    }

    @Override
    public List<Role> getRoles() {
         getKeycloakRoles();
         return (List<Role>) roleRepository.findAll();
    }

    @Override
    public void addRole(Role role) {
        if(!haveInRepo(role)){
            try {
                addKeycloakRole(role);
                roleRepository.save(role);
            }
            catch (ClientErrorException e){

            }
        }

    }

    @Override
    public void deleteRole(Role role) {
        Role toDelete = findInRepo(role);
        if (toDelete!=null) {
            deleteKeycloakRole(role);
            roleRepository.delete(toDelete);
        }

    }

    private Role findInRepo(Role role) {
        List<Role> roles = (List<Role>) roleRepository.findAll();
        for(Role r: roles){
            if (r.getName().equals(role.getName()))
                    return r;
        }
        return null;
    }

    @Override
    public void deleteRole(long roleId) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundException());
           deleteKeycloakRole(role);
           roleRepository.delete(role);
    }

    @Override
    public Role updateRole(Long roleId, Role roleDetailes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException());
        updateKeycloakRole(role,roleDetailes);
        final Role updatedRole = updateRepo(roleDetailes, role);
        return updatedRole;
    }

    private Role updateRepo(Role newRole, Role oldRole) {
        oldRole.setName(newRole.getName());
        return roleRepository.save(oldRole);
    }

    private void updateKeycloakRole(Role role,Role update) {
        RealmResource relamResource = keycloak.realm("Admin");
        RolesResource roles =  relamResource.roles();
        RoleResource beforeRole  = roles.get(role.getName());
        RoleRepresentation newRep = new RoleRepresentation();
        newRep.setName(update.getName());
        beforeRole.update(newRep);
    }


}
