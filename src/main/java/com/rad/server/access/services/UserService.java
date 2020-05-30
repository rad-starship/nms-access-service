/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.UserRepository;
import org.springframework.stereotype.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author raz_o
 *
 */
@Service
public interface UserService
{
    void addKeycloakUser(User user, ArrayList<String> tenant, String role);

    void deleteKeycloakUser(String username,String tenant);

    void updateKeycloakUser(User user,String userName);

    void initKeycloakUsers(UserRepository userRepository);
}
