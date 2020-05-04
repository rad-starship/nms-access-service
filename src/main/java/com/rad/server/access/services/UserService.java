/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.User;
import org.springframework.stereotype.*;

import java.util.List;

/**
 * @author raz_o
 *
 */
@Service
public interface UserService
{
    void addKeycloakUser(User user,String tenant,String role);

    List<User> getKeycloakUsers();

    void deleteKeycloakUser(String username,String tenant);

    void updateKeycloakUser(User user,String userName);
}
