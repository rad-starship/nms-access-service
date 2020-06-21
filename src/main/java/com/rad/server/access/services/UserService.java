/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author raz_o
 *
 */
@Service
public interface UserService
{
    List<User> getUsers();

    Object getUser(long id);

    int addKeycloakUser(User user, ArrayList<String> tenant, String role);

    ResponseEntity<?> deleteKeycloakUser(long id);

    boolean updateKeycloakUser(User user,String userName,String password,String realm);

    void initKeycloakUsers(UserRepository userRepository);

    Object getContinentsByToken(String username);

    Object addUser(User user);

    ResponseEntity<?> updateUser(long id,User user);

}
