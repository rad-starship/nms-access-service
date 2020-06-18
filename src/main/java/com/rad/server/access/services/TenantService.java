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

    Object getTenants();

    Object addKeycloakTenant(Tenant Tenant);

    Object deleteKeycloakTenant(long id);

    Object updateKeycloakTenant(Tenant tenant,long id);

    boolean tenantExists(Tenant tenant);

    void initKeycloakTenants(TenantRepository repository);

    void addIdentityProvider(String providerID,String secret,String clientID,String realm);
}
