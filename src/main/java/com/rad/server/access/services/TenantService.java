/**
 * 
 */
package com.rad.server.access.services;

import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.TenantRepository;
import org.springframework.stereotype.*;

import java.util.List;

/**
 * @author raz_o
 *
 */
@Service
public interface TenantService
{
    void addKeycloakTenant(Tenant Tenant);

    List<User> getKeycloakTenants();

    void deleteKeycloakTenant(String name);

    void updateKeycloakTenant(Tenant tenant,String name);

    boolean tenantExists(Tenant tenant);
    void initKeycloakTenants(TenantRepository repository);
}
