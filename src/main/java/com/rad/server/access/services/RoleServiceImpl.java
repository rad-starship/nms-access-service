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
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
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

    @Autowired
    private AccessToken token;


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
         List<Role> roles = getKeycloakRoles();
         return getComposites(roles);
    }

    @Override
    public List<Map<String, String>> getPermissions() {
        getKeycloakRoles();
        return getNonComposites();
    }

    /**
     * The function goes over all roles on RoleRepository and get all non composite ones.
     * @return list of maps, each map contains role name and id.
     */
    private List<Map<String, String>> getNonComposites() {
        List<Map<String,String>> output = new ArrayList<>();
        for (Role role :roleRepository.findAll()){
            if (role.getPermissions().size()==0 && !role.getName().equals("offline_access") && !role.getName().equals("uma_authorization")){
                Map<String,String> record = new ConcurrentHashMap<>();
                record.put("name",role.getName());
                record.put("Id",String.valueOf(role.getId()));
                output.add(record);
            }
        }
        return output;

    }

    /**
     * Save role on repository and keycloak, on server init. hence add role to all realms.
     * @param role - the role that need to be saved
     */
    @Override
    public void initRole(Role role) {
        System.out.println("Init Role.");
        Role r =roleRepository.save(role);
        try{
            addAllKeycloakRole(role);
        }
        catch(Exception e){
            throw e;
        }

    }

    /**
     * The function goes over allRoles and finds all composite roles
     * @param allRoles - list of roles
     * @return list of composite roles.
     */
    private List<Role> getComposites(List<Role> allRoles) {
        List<Role> output = new LinkedList<>();
        for (Role role :allRoles){
            if (role.getPermissions().size()>0){
                output.add(role);
            }
        }
        return output;
    }

    /**
     * Try to add new role into repository and keycloak for specific token realm.
     * @param role - role to be added.
     */
    @Override
    public void addRole(Role role) throws Exception {
        if(!haveInRepo(role)){

                addRoleToTenant(role,getRealmFromToken());
                roleRepository.save(role);


        }
        else {
            throw new Exception("Role is exist");
        }
    }


    @Override
    public void deleteRole(Role role) {
        Role toDelete = findInRepo(role);
        if (toDelete!=null) {
            deleteKeycloakRole(role);
            roleRepository.delete(toDelete);
        }
        else
            throw new NotFoundException();

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

    /**
     * Search Specific role in RoleRepository by its name.
     * @param role - the role we look for
     * @return The Role representation inside the repository , null if not exist.
     */
    private Role findInRepo(Role role) {
        List<Role> roles = (List<Role>) roleRepository.findAll();
        for(Role r: roles){
            if (r.getName().equals(role.getName()))
                    return r;
        }
        return null;
    }

    /**
     * The function receives role and returns true if he is inside Role repository, return false otherwise.
     */
    private  boolean  haveInRepo(Role role) {
        return findInRepo(role) != null;
    }

    /**
     * Updates role data in repository.
     * @param newRole - the updated data
     * @param oldRole - the old data
     * @return the updated Role
     */
    private Role updateRepo(Role newRole, Role oldRole) {
        oldRole.setName(newRole.getName());
        oldRole.addPermission(newRole.getPermissions());
        return roleRepository.save(oldRole);
    }

    /**
     * The function update Role repository by accessing keycloak and get all roles of current realm
     * @return list of roles form relevant realm.
     */
    public List<Role> getKeycloakRoles() {
        List<Role> rolesInKc = new ArrayList<>();
        Keycloak keycloak = getKeycloak();
        String realm = getRealmFromToken();
        RealmResource relamResource = keycloak.realm(realm);
        RolesResource roles =  relamResource.roles();
        roles.list().forEach(role->
        {

            Role newRole = new Role( role.getName());
            if(role.isComposite()){
                List<String> permissions = new LinkedList<>();
                for (RoleRepresentation permission :roles.get(role.getName()).getRoleComposites()){
                    permissions.add(permission.getName());
                }
                newRole.addPermission(permissions);
            }
            synchronized(this) {
                if (!haveInRepo(newRole))
                    roleRepository.save(newRole);
            }
            rolesInKc.add(findInRepo(newRole));
        });
        return rolesInKc;
    }

    /**
     * This function get realm out of the access token from issuer field
     * @return realm name.
     */
    private String getRealmFromToken() {
        String[] headers = token.getIssuer().split("/");
        String realm = headers[headers.length - 1];
        return realm;

    }

    /**
     * Add keycloak role to all realms
     * @param role - the role to add.
     */
    public void addAllKeycloakRole(Role role) {

        try {
            for (Tenant t : tenantRepository.findAll()) {
                addRoleToTenant(role, t.getName());
            }
        }
        catch (Exception e){
            throw e;
        }

    }

    /**
     *  Add keycloak role to specific realm.
     * @param role - the role to add.
     * @param tenant - the realm name.
     */
    private void addRoleToTenant(Role role, String tenant) {
        Keycloak keycloak = getKeycloak();
        RealmResource realmResource = keycloak.realm(tenant);
        RoleRepresentation newRole = new RoleRepresentation();
        try {
            List<RoleRepresentation> permissions = getPermissions(role.getPermissions(),tenant);
            newRole.setName(role.getName());
            realmResource.roles().create(newRole);
            if (permissions.size()>0) {
                realmResource.roles().get(role.getName()).toRepresentation().setComposite(true);
                realmResource.roles().get(role.getName()).addComposites(permissions);
            }
        }
        catch (ProcessingException e){
            throw e;
        }
        catch (Exception e){

        }
    }

    /**
     * get all keycloak representation of permissions in specific tenant
     * @param permissions - list of inside server permissions.
     * @param tenant - realm name.
     * @return list of keycloak role representation of the permissions.
     */
    private List<RoleRepresentation> getPermissions(List<String> permissions,String tenant) {
        Keycloak keycloak = getKeycloak();
        RealmResource relamResource = keycloak.realm(tenant);
        List<RoleRepresentation> output = new LinkedList<>();
        for (String r : permissions){
            RoleRepresentation role = relamResource.roles().get(r).toRepresentation();
            output.add(role);
        }
        return output;
    }

    /**
     * Update keycloak role on specific tenant
     * @param role - the old role.
     * @param update - the updated role.
     */
    private void updateKeycloakRole(Role role,Role update) {
        Keycloak keycloak = getKeycloak();
        RealmResource relamResource = keycloak.realm(getRealmFromToken());
        RolesResource roles =  relamResource.roles();
        RoleResource beforeRole  = roles.get(role.getName());
        RoleRepresentation newRep = new RoleRepresentation();
        newRep.setName(update.getName());
        newRep.setComposite(true);

        List<RoleRepresentation> oldComposites = new ArrayList<>(beforeRole.getRoleComposites());
        List<RoleRepresentation> newComposites = getPermissions(update.getPermissions(),getRealmFromToken());
        if(!oldComposites.isEmpty())
            beforeRole.deleteComposites(oldComposites);
        beforeRole.addComposites(newComposites);
        beforeRole.update(newRep);




    }

    /**
     * Delete keycloak role of specific realm
     * @param role - the role to be deleted.
     */
    private void deleteKeycloakRole(Role role) {
        Keycloak keycloak = getKeycloak();
        RealmResource realmResource = keycloak.realm(getRealmFromToken());
        realmResource.roles().deleteRole(role.getName());
    }

}
