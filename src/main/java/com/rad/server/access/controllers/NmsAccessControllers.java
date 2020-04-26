package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.services.RoleService;
import com.rad.server.access.services.UserService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.repositories.*;

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
	private RoleRepository		roleRepository;
	
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
		List<Role> roles = roleService.getKeycloakRoles();
		System.out.println("getRoles: " + roles);

		return roles;
	}

	@PostMapping("/roles")
	@ResponseBody
	public Role addRole(@RequestBody Role role)
	{
		System.out.println("addRole: " + role);
		//roleRepository.save(role);
		roleService.addKeycloakRole(role);
		return role;
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
		System.out.println("addRole: " + tenant);
		tenantRepository.save(tenant);
		return tenant;
	}

	private User getUserFromRepository(long id) {
		Optional<User> userExists = userRepository.findById(id);
		return userExists.orElse(null);
	}
}