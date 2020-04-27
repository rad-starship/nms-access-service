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
    void addKeycloakUser(User user);

    List<User> getKeycloakUsers();

    void deleteKeycloakUser(String id);

    void updateKeycloakUser(User user,String userName);
}
