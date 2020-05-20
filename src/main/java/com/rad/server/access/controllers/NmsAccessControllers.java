package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.entities.settings.Settings;
import com.rad.server.access.responses.HttpResponse;
import com.rad.server.access.services.*;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.repositories.*;

import javax.management.InstanceAlreadyExistsException;
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
    private SettingsService settingsService;

	@Autowired
	private AccessTokenService accessTokenService;

	@Autowired
	private UserRepository	userRepository;

	@Autowired
	private TenantRepository	tenantRepository;

	@Autowired
	private RoleRepository	roleRepository;

	@Autowired
	private AccessToken token;



	@GetMapping("/users")
	@ResponseBody
	public List<User> getUsers()
	{
		List<User> users =(List<User>) userRepository.findAll();
		System.out.println("getUsers: " + users);
		return users;
	}

	@GetMapping("/users/getToken")
	@ResponseBody
	public ArrayList<String> getUserToken()
	{
		User tokenUser=getUserFromToken(token.getPreferredUsername());
		ArrayList<String> tenants=new ArrayList<>();
		for(long id:tokenUser.getTenantID()){
			tenants.add(tenantRepository.findById(id).get().getName());
		}
		return tenants;
	}

	@PostMapping("/users")
	@ResponseBody
	public Object addUser(@RequestBody User user)
	{
		try {
			if(!isTokenUserFromSameTenant(user)){
				throw new Error();
			}
			ArrayList<String> realms=new ArrayList<>();
			for (Long tenant:user.getTenantID()) {
				if(tenantRepository.existsById(tenant)){
					realms.add(tenantRepository.findById(tenant).get().getName());
				}
				else{
					throw new InstanceAlreadyExistsException();
				}
			}
				if(roleRepository.existsById(user.getRoleID())){
					Role role=roleRepository.findById(user.getRoleID()).get();
					userRepository.save(user);
					userService.addKeycloakUser(user,realms,role.getName());
					return user;
				}
				else throw new NotFoundException();

		}
		catch (NotFoundException e){
			HashMap<String,String> response= new HashMap<>();
			response.put("Data","Tenant ID or Role ID Does not exists");
			return  response;
		}
		catch (InstanceAlreadyExistsException e){
			HashMap<String,String> response= new HashMap<>();
			response.put("Data","Username already exists");
			return  response;
		}
		catch (java.lang.Error e){
			HashMap<String,String> response= new HashMap<>();
			response.put("Data","keycloak user not authorized");
			return  response;
		}
	}

	@DeleteMapping("/users/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteUser(@PathVariable long id){
		User user;
		user=getUserFromRepository(id);
		if(!isTokenUserFromSameTenant(user))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"user and token not from same tenant").getHttpResponse();
		if(user!=null) {
			if(roleRepository.existsById(user.getRoleID())){
				Role userRole=roleRepository.findById(user.getRoleID()).get();
				if(userRole.getName().equals("Admin"))
					return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
			}
			for (Long tenants: user.getTenantID()) {
				Tenant tenant=getTenantFromRepository(tenants);
				if(tenant==null)
					return new HttpResponse(HttpStatus.BAD_REQUEST,"tenant is null").getHttpResponse();
				userService.deleteKeycloakUser(user.getUserName(),tenant.getName());
			}
				System.out.println("User deleted successfully.");
				ResponseEntity<User> result = new ResponseEntity<>(user,HttpStatus.ACCEPTED);
				return new HttpResponse(result).getHttpResponse();
		}
		else
			System.out.println("The user doesnt exist.");
			return new HttpResponse(HttpStatus.NO_CONTENT,"user Doesnt Exist").getHttpResponse();
	}

	@PutMapping("/users/{id}")
	@ResponseBody
	public User updateUser(@PathVariable long id,@RequestBody User user){
		User oldUser=getUserFromRepository(id);
		if(!isTokenUserFromSameTenant(oldUser))
			return null;
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
	public ResponseEntity<?> deleteRole(@PathVariable(value = "name") String roleName){

	System.out.println("DeleteRole: " + roleName);
	try {
		if(roleName.equals("Admin")){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
		}
			roleService.deleteRole(new Role(roleName));
			return new ResponseEntity<String>("Success",HttpStatus.ACCEPTED);
	}
	catch(NotFoundException e){
		return new  ResponseEntity<String>("Not found",HttpStatus.NO_CONTENT);
	}



}

	@DeleteMapping("/rolesid/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteRole(@PathVariable(value = "id") long roleId){
		System.out.println("DeleteRole: " + roleId);
		try {
			if(roleRepository.existsById(roleId)){
				if(roleRepository.findById(roleId).get().getName().equals("Admin"))
					return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
			}
			roleService.deleteRole(roleId);
			ResponseEntity<Long> result = new ResponseEntity<>(roleId,HttpStatus.ACCEPTED);
			return new HttpResponse(result).getHttpResponse();
		}
		catch(NotFoundException e){
			ResponseEntity<Long> result = new ResponseEntity<>(roleId,HttpStatus.NO_CONTENT);
			return new HttpResponse(result).getHttpResponse();
		}


	}


	@GetMapping("/permissions")
	@ResponseBody
	public List<Map<String,String>> getPermissions(){
		List<Map<String,String>> permissions = roleService.getPermissions();
		return permissions;
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
		catch (Exception e){
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
			if(tenant.getName().equals("Admin"))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
			tenantRepository.delete(tenant);
			tenantService.deleteKeycloakTenant(tenant.getName());
			ResponseEntity<Tenant> result = new ResponseEntity<Tenant>(tenant,HttpStatus.ACCEPTED);
			return result;
		}
		else
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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

	@PostMapping("/settings")
    @ResponseBody
    public Object postSettings(@RequestBody Object settings){
        System.out.println(settings);

		Settings settings1 = settingsService.parseSettings(settings);
		settingsService.applySettings(settings1);

		return settings;

    }

    @PostMapping("/getToken")
	@ResponseBody
	public Object login(@RequestBody(required = false) LoginEntity loginEntity){
		System.out.println(loginEntity);
		return accessTokenService.getAccessToken(loginEntity);
	}


	private User getUserFromRepository(long id) {
		Optional<User> userExists = userRepository.findById(id);
		return userExists.orElse(null);
	}

	private Tenant getTenantFromRepository(long id) {
		Optional<Tenant> tenantExists = tenantRepository.findById(id);
		return tenantExists.orElse(null);
	}

	private Role getRoleFromRepository(long id) {
		Optional<Role> roleExists = roleRepository.findById(id);
		return roleExists.orElse(null);
	}

	private User getUserFromToken(String username){
		for (User user: userRepository.findAll()) {
			if(user.getUserName().equals(username))
				return user;
		}
		return null;
	}

	private boolean isTokenUserFromSameTenant(User user){
		User tokenUser=getUserFromToken(user.getUserName());
		if(tokenUser==null)
			return false;
		Role tokenRole=getRoleFromRepository(tokenUser.getRoleID());
		if(tokenRole==null)
			return false;
		if(tokenRole.getName().equals("Region-Admin")){
			if(!tokenUser.getTenantID().containsAll(user.getTenantID())){
				return false;
			}
		}
		return true;
	}

}