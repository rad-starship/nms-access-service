/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Role;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.Map;

/**
 * @author raz_o
 *
 */
@Service
public interface RoleService
{
    List<Role> getRoles();
    List<Map<String, String>> getPermissions();

    void initRole(Role role);

    void addRole(Role role) throws Exception;

    void deleteRole(Role role);

    void deleteRole(long roleId);

    Role updateRole(Long roleId, Role roleDetailes);


}
