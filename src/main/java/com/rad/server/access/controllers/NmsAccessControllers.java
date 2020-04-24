package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.services.RoleService;
import com.rad.server.access.services.UserService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.repositories.*;

import javax.ws.rs.NotFoundException;

/**
 * @author raz_o
 */
@Controller
public class NmsAccessControllers
{
	@Autowired
	private RoleService roleService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository		userRepository;
	

	
	@Autowired
	private TenantRepository	tenantRepository;

	@GetMapping("/users")
	@ResponseBody
	public List<User> getUsers()
	{
		List<User> users =(List<User>) userRepository.findAll();
		System.out.println("getUsers: " + users);
		return users;
	}

	@PostMapping("/users")
	@ResponseBody
	public User addUser(@RequestBody User user)
	{
		System.out.println("addUser: " + user);
		userRepository.save(user);
		userService.addKeycloakUser(user);
		return user;
	}

	@DeleteMapping("/users/{id}")
	@ResponseBody
	public User deleteUser(@PathVariable long id){
		User user;
		Optional<User> userExists=userRepository.findById(id);
		if(userExists.isPresent()) {
			user = userExists.get();
			userRepository.delete(user);
			userService.deleteKeycloakUser(id);
			System.out.println("User deleted successfully.");
			return user;
		}
		else
			System.out.println("The user doesnt exist.");
		return null;
	}
	
	@GetMapping("/roles")
	@ResponseBody
	public List<Role> getRoles()
	{
		//List<Role> roles = (List<Role>) roleRepository.findAll();
		List<Role> roles = roleService.getRoles();
		System.out.println("getRoles: " + roles);

		return roles;
	}

	@PostMapping("/roles")
	@ResponseBody
	public Role addRole(@RequestBody Role role)
	{
		System.out.println("addRole: " + role);
		//roleRepository.save(role);
		roleService.addRole(role);
		return role;
	}



	@DeleteMapping("/roles/{name}")
	@ResponseBody
	public Map<String, Boolean> deleteRole(@PathVariable(value = "name") String roleName){
	Map<String, Boolean> response = new HashMap<>();
	System.out.println("DeleteRole: " + roleName);
	try {
		roleService.deleteRole(new Role(roleName));
		response.put("deleted", Boolean.TRUE);
	}
	catch(NotFoundException e){
		response.put("deleted",Boolean.FALSE);
	}

	return response;

}

	@DeleteMapping("/rolesid/{id}")
	@ResponseBody
	public Map<String, Boolean> deleteRole(@PathVariable(value = "id") long roleId){
		Map<String, Boolean> response = new HashMap<>();
		System.out.println("DeleteRole: " + roleId);
		try {
			roleService.deleteRole(roleId);
			response.put("deleted", Boolean.TRUE);
		}
		catch(NotFoundException e){
			response.put("deleted",Boolean.FALSE);
		}

		return response;

	}

	
	@GetMapping("/tenants")
	@ResponseBody
	public List<Tenant> getTenants()
	{
		List<Tenant> tenants = (List<Tenant>) tenantRepository.findAll();
		System.out.println("getTenants: " + tenants);
		return tenants;
	}

	@PostMapping("/tenants")
	@ResponseBody
	public Tenant addTenant(@RequestBody Tenant tenant)
	{
		System.out.println("addTenant: " + tenant);
		tenantRepository.save(tenant);
		return tenant;
	}
}