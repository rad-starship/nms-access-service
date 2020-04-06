/**
 * 
 */
package com.rad.server.access.repositories;

import org.springframework.data.repository.*;
import org.springframework.stereotype.Repository;
import com.rad.server.access.entities.*;

@Repository
public interface TenantRepository extends CrudRepository<Tenant, Long>
{
}