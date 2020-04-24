/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import org.springframework.stereotype.*;

import java.util.List;

/**
 * @author raz_o
 *
 */
@Service
public interface RoleService
{
    List<Role> getRoles();

    void addRole(Role role);

    void deleteRole(Role role);

    void deleteRole(long roleId);
}
