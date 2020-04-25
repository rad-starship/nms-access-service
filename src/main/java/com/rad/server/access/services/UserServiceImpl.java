package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import javax.websocket.OnClose;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class UserServiceImpl implements UserService {



    private Keycloak getKeycloakInstance(){
        Keycloak keycloak = Keycloak.getInstance(
                "http://localhost:8080/auth",// keycloak address
                "master", // ​​specify Realm master
                "avielavitan", // ​​administrator account
                "aviel5002", // ​​administrator password
                "admin-cli");
        return keycloak;
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
    public void deleteKeycloakUser(String userName){
        Keycloak keycloak=getKeycloakInstance();
        RealmResource realmResource = keycloak.realm("Admin");
        UsersResource users =  realmResource.users();
        users.delete(users.search(userName).get(0).getId());
    }

    @Override
    public void addKeycloakUser(User user) {
        Keycloak keycloak = getKeycloakInstance();
        UserRepresentation userRep= new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername(user.getUserName());
        userRep.setFirstName(user.getFirstName());
        userRep.setLastName(user.getLastName());
        userRep.setEmail(user.getEmail());
        userRep.setAttributes(Collections.singletonMap("origin", Arrays.asList("demo")));
        RealmResource realmResource = keycloak.realm("Admin");

        UsersResource usersResource = realmResource.users();
        Response response=usersResource.create(userRep);

        System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());

    }

    public UserRepresentation getUserRepFromList(List<UserRepresentation> urList,String user){
        for (UserRepresentation ur: urList) {
            if(ur.getUsername().equals(user))
                return ur;
        }
        return null;
    }


}