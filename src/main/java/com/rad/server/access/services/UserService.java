/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.UserRepository;
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
    int addKeycloakUser(User user, ArrayList<String> tenant, String role);

    void deleteKeycloakUser(String username,String tenant);

    boolean updateKeycloakUser(User user,String userName,String password,String realm);

    void initKeycloakUsers(UserRepository userRepository);
}
