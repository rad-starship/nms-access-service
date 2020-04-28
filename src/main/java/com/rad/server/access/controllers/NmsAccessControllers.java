package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.services.RoleService;
import com.rad.server.access.services.TenantService;
import com.rad.server.access.services.UserService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.repositories.*;

import javax.validation.Valid;
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
	private TenantService tenantService;

	@Autowired
	private UserRepository	userRepository;

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
	public Object addUser(@RequestBody User user)
	{
		try {
			System.out.println("addUser: " + user);
			userRepository.save(user);
			userService.addKeycloakUser(user);
			return user;
		}

		catch (DataIntegrityViolationException e){
			HashMap<String,String> response= new HashMap<>();
			response.put("Data","Username already exists");
			return  response;
		}
	}

	@DeleteMapping("/users/{id}")
	@ResponseBody
	public User deleteUser(@PathVariable long id){
		User user;
		user=getUserFromRepository(id);
		if(user!=null) {
			userRepository.delete(user);
			userService.deleteKeycloakUser(user.getUserName());
			System.out.println("User deleted successfully.");
			return user;
		}
		else
			System.out.println("The user doesnt exist.");
		return null;
	}

	@PutMapping("/users/{id}")
	@ResponseBody
	public User updateUser(@PathVariable long id,@RequestBody User user){
		User oldUser=getUserFromRepository(id);
		if(oldUser==null)
			return null;
		User newUser=new User(user);
		newUser.setId(id);
		newUser.setUserName(oldUser.getUserName());
		userService.updateKeycloakUser(user,oldUser.getUserName());
		userRepository.save(newUser);
		return user;
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


	@PutMapping("/roles/{id}")
	@ResponseBody
	public Role updateRole(@PathVariable(value = "id") Long roleId,
												   @Valid @RequestBody Role roleDetailes) {
		try {
			return roleService.updateRole(roleId, roleDetailes);
		}
			catch(Exception e){
			return new Role("NotFound");
		}
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
	public Object addTenant(@RequestBody Tenant tenant)
	{
		try {

			System.out.println("addTenant: " + tenant);
			tenantRepository.save(tenant);
			tenantService.addKeycloakTenant(tenant);
			return tenant;
		}
		catch (DataIntegrityViolationException e){
			HashMap<String,String> response= new HashMap<>();
			response.put("Data","Tenant already exists");
			return  response;
		}
	}

	@DeleteMapping("/tenants/{id}")
	@ResponseBody
	public Object deleteTenant(@PathVariable long id){
		Tenant tenant;
		tenant=getTenantFromRepository(id);
		if(tenant!=null) {
			tenantRepository.delete(tenant);
			tenantService.deleteKeycloakTenant(tenant.getName());
			return tenant;
		}
		else
		return null;
	}

	@PutMapping("/tenants/{id}")
	@ResponseBody
	public Object updateTenant(@PathVariable long id,@RequestBody Tenant tenant){
		Tenant oldTenant=getTenantFromRepository(id);
		if(oldTenant==null) {
			HashMap<String,String> response=new HashMap<>();
			response.put("Data","The tenant does not exist");
			return response;
		}
		Tenant newTenant=new Tenant(tenant.getName());
		newTenant.setId(id);
		tenantService.updateKeycloakTenant(tenant,oldTenant.getName());
		tenantRepository.save(newTenant);
		return tenant;
	}


	private User getUserFromRepository(long id) {
		Optional<User> userExists = userRepository.findById(id);
		return userExists.orElse(null);
	}

	private Tenant getTenantFromRepository(long id) {
		Optional<Tenant> tenantExists = tenantRepository.findById(id);
		return tenantExists.orElse(null);
	}
}