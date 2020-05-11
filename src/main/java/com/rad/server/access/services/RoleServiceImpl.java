package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.repositories.RoleRepository;
import com.rad.server.access.repositories.TenantRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

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

    //Overridable CRUD functions:
    @Override
    public  List<Role> getRoles() {
         getKeycloakRoles();
         return getComposites();
    }

    @Override
    public List<Map<String, String>> getPermissions() {
        getKeycloakRoles();
        return getNonComposites();
    }

    private List<Map<String, String>> getNonComposites() {
        List<Map<String,String>> output = new ArrayList<>();
        for (Role role :roleRepository.findAll()){
            if (role.getPermissions().size()==0 && !role.getName().equals("offline_access")){
                Map<String,String> record = new ConcurrentHashMap<>();
                record.put("name",role.getName());
                record.put("Id",String.valueOf(role.getId()));
                output.add(record);
            }
        }
        return output;

    }

    @Override
    public void initRole(Role role) {
        Role r =roleRepository.save(role);
        try{
            addKeycloakRole(role);
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }

    }

    private List<Role> getComposites() {
        List<Role> output = new LinkedList<>();
        for (Role role :roleRepository.findAll()){
            if (role.getPermissions().size()>0){
                output.add(role);
            }
        }
        return output;
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

    //Repo search  by role name
    private Role findInRepo(Role role) {
        List<Role> roles = (List<Role>) roleRepository.findAll();
        for(Role r: roles){
            if (r.getName().equals(role.getName()))
                    return r;
        }
        return null;
    }

    private  boolean  haveInRepo(Role newRole) {
        List<Role> repo = (List<Role>) roleRepository.findAll();
        for(Role r : repo){
            if (r.getName().equals(newRole.getName()))
                return true;
        }
        return false;
    }
    
    //Update data in repo
    private Role updateRepo(Role newRole, Role oldRole) {
        oldRole.setName(newRole.getName());
        oldRole.addPermission(newRole.getPermissions());
        return roleRepository.save(oldRole);
    }

    //Keycloack CRUD functions
    public  void getKeycloakRoles() {
        Keycloak keycloak = getKeycloak();
        RealmResource relamResource = keycloak.realm("Admin");
        RolesResource roles =  relamResource.roles();
        roles.list().forEach(role->
        {

            Role newRole = new Role(role.getId(), role.getName());
            if(role.isComposite()){
                List<String> permissions = new LinkedList<>();
                for (RoleRepresentation permission :roles.get(role.getName()).getRoleComposites()){
                   // Role compositeRole = new Role(permission.getId(),permission.getName());
                   // compositeRole = findInRepo(compositeRole);
                    permissions.add(permission.getName());
                }
                newRole.addPermission(permissions);
            }
            synchronized(this) {
                if (!haveInRepo(newRole))
                    roleRepository.save(newRole);
            }
        });
    }

    public void addKeycloakRole(Role role) {
        Keycloak keycloak = getKeycloak();
        for (Tenant t:tenantRepository.findAll()) {
            RealmResource realmResource = keycloak.realm(t.getName());
            RoleRepresentation newRole = new RoleRepresentation();
            try {
                List<RoleRepresentation> permissions = getPermissions(role.getPermissions());
                newRole.setName(role.getName());
                realmResource.roles().create(newRole);
                if (permissions.size()>0) {
                    realmResource.roles().get(role.getName()).toRepresentation().setComposite(true);
                    realmResource.roles().get(role.getName()).addComposites(permissions);
                }
            }
            catch (Exception e){
                System.out.println("An error Happen");
            }
        }


    }

    private List<RoleRepresentation> getPermissions(List<String> permissions) {
        Keycloak keycloak = getKeycloak();
        RealmResource relamResource = keycloak.realm("Admin");
        List<RoleRepresentation> output = new LinkedList<>();
        for (String r : permissions){
            RoleRepresentation role = relamResource.roles().get(r).toRepresentation();
            output.add(role);
        }
        return output;
    }

    private void updateKeycloakRole(Role role,Role update) {
        Keycloak keycloak = getKeycloak();
        RealmResource relamResource = keycloak.realm("Admin");
        RolesResource roles =  relamResource.roles();
        RoleResource beforeRole  = roles.get(role.getName());
        RoleRepresentation newRep = new RoleRepresentation();
        newRep.setName(update.getName());

        List<RoleRepresentation> oldComposites = new ArrayList<>(beforeRole.getRoleComposites());
        List<RoleRepresentation> newComposites = getPermissions(update.getPermissions());

        beforeRole.update(newRep);
        if(!oldComposites.isEmpty())
            beforeRole.deleteComposites(oldComposites);
        beforeRole.addComposites(newComposites);


    }

    private void deleteKeycloakRole(Role role) {
        Keycloak keycloak = getKeycloak();
        RealmResource realmResource = keycloak.realm("Admin");
        realmResource.roles().deleteRole(role.getName());
    }

}
