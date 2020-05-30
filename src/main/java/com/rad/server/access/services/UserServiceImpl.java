package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.RoleRepository;
import com.rad.server.access.repositories.TenantRepository;
import com.rad.server.access.repositories.UserRepository;
import org.apache.commons.codec.binary.Base64;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.InstanceAlreadyExistsException;
import javax.websocket.OnClose;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Stream;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    AccessToken token;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;






    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }

    /**
     * This function deletes the user from the keycloak servers by username and realm
     * @param userName The deleted username
     * @param tenant The tenant the user belongs to
     */

    @Override
    public void deleteKeycloakUser(String userName,String tenant){
        Keycloak keycloak=getKeycloakInstance();
        RealmResource realmResource = keycloak.realm(tenant);
        UsersResource users =  realmResource.users();
        users.delete(users.search(userName).get(0).getId());
    }


    /**
     * This function adds a new user to the keycloak serve, adds his permissions,
     * and applies the necessary settings
     * @param user
     * @param tenants
     * @param role
     */
    @Override
    public void addKeycloakUser(User user,ArrayList<String> tenants,String role) {
        Keycloak keycloak = getKeycloakInstance();

        for (String tenant:tenants) {
            List<CredentialRepresentation> credentials=new ArrayList<>();
            UserRepresentation userRep= new UserRepresentation();
            userRep.setEnabled(true);
            userRep.setUsername(user.getUserName());
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            userRep.setEmail(user.getEmail());
            userRep.singleAttribute("Username",user.getUserName());
            userRep.singleAttribute("role",role);
            userRep.singleAttribute("realm",tenant);
            CredentialRepresentation credentialRepresentation=new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(user.getPassword());
            credentialRepresentation.setTemporary(false);
            credentials.add(credentialRepresentation);
            userRep.setCredentials(credentials);
            RealmResource realmResource = keycloak.realm(tenant);
            UsersResource usersResource = realmResource.users();
            Response response=usersResource.create(userRep);
            if(response.getStatus()!=400) {
                UserRepresentation addUserRole = usersResource.search(user.getUserName()).get(0);
                UserResource updateUser = usersResource.get(addUserRole.getId());
                List<RoleRepresentation> roleRepresentationList = updateUser.roles().realmLevel().listAvailable();

                for (RoleRepresentation roleRepresentation : roleRepresentationList) {
                    if (roleRepresentation.getName().equals(role)) {
                        updateUser.roles().realmLevel().add(Arrays.asList(roleRepresentation));
                        break;
                    }
                }
            }
            System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
        }
    }

    /**
     * This function updates an existing user in the keycloak server by username
     * @param user
     * @param userName
     */
    public void updateKeycloakUser(User user ,String userName){
        Keycloak keycloak=getKeycloakInstance();
        List<CredentialRepresentation> credentials=new ArrayList<>();
        RealmResource realmResource = keycloak.realm("Admin");
        UsersResource users =  realmResource.users();
        UserRepresentation userRep=users.search(userName).get(0);
        userRep.setEmail(user.getEmail());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        CredentialRepresentation credentialRepresentation=new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(user.getPassword());
        credentialRepresentation.setTemporary(false);
        credentials.add(credentialRepresentation);
        userRep.setCredentials(credentials);
        UserResource updateUser=users.get(userRep.getId());
        updateUser.update(userRep);
    }

    /**
     * This function initializes the keycloak server users and also initializes the repository
     * with new users that are in the keycloak server
     * @param userRepository
     */
    public void initKeycloakUsers(UserRepository userRepository){
        Keycloak keycloak=getKeycloakInstance();
        RealmsResource realms=keycloak.realms();
        List<RealmRepresentation> existingRealms=realms.findAll();
        for(RealmRepresentation r:existingRealms){
            if(r.getRealm().equals("master"))
                continue;
            UsersResource users=keycloak.realm(r.getRealm()).users();
            for(UserRepresentation u:users.list()){
                if(!userExistsInRepository(u.getUsername(),r.getRealm(),userRepository)) {
                    ArrayList<Long> realmsToAdd=new ArrayList<>();
                    realmsToAdd.add(getTenantID(r.getRealm()));
                    User addUser=new User(u.getFirstName(),u.getLastName(),u.getEmail(),u.getUsername(),"",getRoleID(keycloak.realm(r.getRealm()).users().get(u.getId()).roles().realmLevel().listEffective()),realmsToAdd);
                    userRepository.save(addUser);
                }
            }
        }

    }

    /**
     * This function checks if the user exists in the user repository by username and tenant
     * @param username
     * @param realm
     * @param userRepository
     * @return
     */
    public boolean userExistsInRepository(String username,String realm,UserRepository userRepository){
        for(User user:userRepository.findAll()){
            if(user.getUserName().toLowerCase().equals(username)){
                long tenantID=getTenantID(realm);
                for(long id:user.getTenantID()){
                    if(id==tenantID)
                        return true;
                }
                user.addTenant(tenantID);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private long getTenantID(String tenant){
        for(Tenant t:tenantRepository.findAll()){
            if(t.getName().equals(tenant))
               return t.getId();
        }
        return 0;
    }

    private long getRoleID(List<RoleRepresentation> roles){
        for(RoleRepresentation role:roles){
            for(Role r:roleRepository.findAll()){
                if(r.getName().equals(role.getName()))
                   return r.getId();
            }
        }

        return 0;
    }

}