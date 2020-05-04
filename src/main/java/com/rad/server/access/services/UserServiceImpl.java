package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.websocket.OnClose;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Stream;

@Service
public class UserServiceImpl implements UserService {

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
    public List<User> getKeycloakUsers() {
        List<User> output = new LinkedList<>();
        Keycloak keycloak = getKeycloakInstance();
        RealmResource realmResource = keycloak.realm("Admin");
        UsersResource users =  realmResource.users();
        users.list().forEach(user->output.add(new User(user.getFirstName(),user.getLastName(),user.getEmail(),user.getUsername())));
        return output;
    }

    @Override
    public void deleteKeycloakUser(String userName,String tenant){
        Keycloak keycloak=getKeycloakInstance();
        RealmResource realmResource = keycloak.realm(tenant);
        UsersResource users =  realmResource.users();
        users.delete(users.search(userName).get(0).getId());
    }

    @Override
    public void addKeycloakUser(User user,String tenant,String role) {
        Keycloak keycloak = getKeycloakInstance();
        UserRepresentation userRep= new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername(user.getUserName());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        userRep.setEmail(user.getEmail());
        userRep.setAttributes(Collections.singletonMap("origin", Arrays.asList("demo")));
        RealmResource realmResource = keycloak.realm(tenant);
        UsersResource usersResource = realmResource.users();
        Response response=usersResource.create(userRep);
        UserRepresentation addUserRole=usersResource.search(user.getUserName()).get(0);
        UserResource updateUser=usersResource.get(addUserRole.getId());
        List<RoleRepresentation> roleRepresentationList = updateUser.roles().realmLevel().listAvailable();

        for (RoleRepresentation roleRepresentation : roleRepresentationList)
        {
            if (roleRepresentation.getName().equals(role))
            {
                updateUser.roles().realmLevel().add(Arrays.asList(roleRepresentation));
                break;
            }
        }

        System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());

    }

    public void updateKeycloakUser(User user ,String userName){
        Keycloak keycloak=getKeycloakInstance();
        RealmResource realmResource = keycloak.realm("Admin");
        UsersResource users =  realmResource.users();
        UserRepresentation userRep=users.search(userName).get(0);
        userRep.setEmail(user.getEmail());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        UserResource updateUser=users.get(userRep.getId());
        updateUser.update(userRep);
    }

    public UserRepresentation getUserRepFromList(List<UserRepresentation> urList,String user){
        for (UserRepresentation ur: urList) {
            if(ur.getUsername().equals(user))
                return ur;
        }
        return null;
    }


}