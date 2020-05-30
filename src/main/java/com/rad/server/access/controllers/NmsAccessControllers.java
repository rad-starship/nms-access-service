package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.entities.settings.Settings;
import com.rad.server.access.responses.HttpResponse;
import com.rad.server.access.services.*;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.repositories.*;
import org.apache.commons.codec.binary.Base64;

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
	public List<User> getUsers(@RequestHeader HttpHeaders headers)
	{
		List<User> users =(List<User>) userRepository.findAll();
		System.out.println("getUsers: " + users);
		return users;
	}


	/**
	 * This function returns the continent list that the logged in user is allowed to watch in health service
	 * @param headers
	 * @param username
	 * @return
	 */

	@GetMapping("/users/getTokenTenants/{username}")
	@ResponseBody
	public ArrayList<String> getUserToken(@RequestHeader HttpHeaders headers,@PathVariable String username)
	{
		User tokenUser=getUserFromToken(username);
		ArrayList<String> tenants=new ArrayList<>();
		for(long id:tokenUser.getTenantID()){
			for(String continent:tenantRepository.findById(id).get().getContinents()){
				if(!tenants.contains(continent))
					tenants.add(continent);
			}
		}
		return tenants;
	}

	/**
	 * This function adds a new user to the repository and to keycloak
	 * @param user
	 * @return
	 */
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
					throw new NotFoundException();
				}
			}
				if(roleRepository.existsById(user.getRoleID())){
					Role role=roleRepository.findById(user.getRoleID()).get();
					User exists=getUserFromRepositoryByUsername(user.getUserName());
					if(exists!=null) {
						if(exists.getTenantID().containsAll(user.getTenantID()))
							throw new InstanceAlreadyExistsException();
						else{
							user.setId(exists.getId());
							for(long id:exists.getTenantID()){
								if(!user.getTenantID().contains(id))
									user.getTenantID().add(id);
							}
						}
					}
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

	/**
	 * This function deletes a user from the repository and from keycloak by id if it exists
	 * @param id
	 * @return
	 */

	@DeleteMapping("/users/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteUser(@PathVariable long id){
		User user;
		user=getUserFromRepository(id);
		if(!isTokenUserFromSameTenant(user))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
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


	/**
	 * This function updates a registered user by id if it exists
	 * @param id
	 * @param user The new user details
	 * @return
	 */
	@PutMapping("/users/{id}")
	@ResponseBody
	public ResponseEntity<?> updateUser(@PathVariable long id,@RequestBody User user){
		User oldUser=getUserFromRepository(id);
		if(!isTokenUserFromSameTenant(oldUser))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
		if(oldUser==null)
			return new HttpResponse(HttpStatus.BAD_REQUEST,"User doesnt exists").getHttpResponse();
		User newUser=new User(user);
		newUser.setId(id);
		newUser.setUserName(oldUser.getUserName());
		userService.updateKeycloakUser(user,oldUser.getUserName());
		userRepository.save(newUser);
		ResponseEntity<User> result = new ResponseEntity<>(user,HttpStatus.ACCEPTED);
		return new HttpResponse(result).getHttpResponse();
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


	/**
	 * This function adds a new role to the repository, and to all of the keycloak tenants
	 * @param role
	 * @return
	 */
	@PostMapping("/roles")
	@ResponseBody
	public Role addRole(@RequestBody Role role)
	{
		System.out.println("addRole: " + role);
		//roleRepository.save(role);
		roleService.addRole(role);
		return role;
	}

	/**
	 * This function updates an existing role by id if it exists
	 * @param roleId
	 * @param roleDetails The new role details
	 * @return
	 */
	@PutMapping("/roles/{id}")
	@ResponseBody
	public Role updateRole(@PathVariable(value = "id") Long roleId,
												   @Valid @RequestBody Role roleDetails) {
		try {
			return roleService.updateRole(roleId, roleDetails);
		}
			catch(Exception e){
			return new Role("NotFound");
		}
	}


	/**
	 * This function deletes a role from the repository and the keycloak servers by role name
	 * @param roleName
	 * @return
	 */
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
	/**
	 * This function deletes a role from the repository and the keycloak servers by role ID
	 * @param roleId
	 * @return
	 */
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


	/**
	 * This function adds a new tenant to the repository and to the keycloak servers
	 * @param tenant
	 * @return
	 */
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


	/**
	 * This function deletes a tenant from the repository and the keycloak servers by ID, if it exists
	 * @param id
	 * @return
	 */
	@DeleteMapping("/tenants/{id}")
	@ResponseBody
	public Object deleteTenant(@PathVariable long id){
		Tenant tenant;
		tenant=getTenantFromRepository(id);
		boolean admin=false;
		if(tenant!=null) {
			if(tenant.getName().equals("Admin"))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
			User tokenUser=getUserFromToken(token.getPreferredUsername());
			if(!getRoleFromRepository(tokenUser.getRoleID()).getPermissions().contains("all")){
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Keycloak user unauthorized").getHttpResponse();
			}
			tenantRepository.delete(tenant);
			tenantService.deleteKeycloakTenant(tenant.getName());
			ResponseEntity<Tenant> result = new ResponseEntity<Tenant>(tenant,HttpStatus.ACCEPTED);
			return result;
		}
		else
			return new HttpResponse(HttpStatus.BAD_REQUEST,"wrong tenant id");
	}


	/**
	 * This function updates an existing tenant by ID
	 * @param id
	 * @param tenant The new tenant details
	 * @return
	 */
	@PutMapping("/tenants/{id}")
	@ResponseBody
	public Object updateTenant(@PathVariable long id,@RequestBody Tenant tenant){
		Tenant oldTenant=getTenantFromRepository(id);
		if(oldTenant==null) {
			HashMap<String,String> response=new HashMap<>();
			response.put("Data","The tenant does not exist");
			return response;
		}
		Tenant newTenant=new Tenant(tenant.getName(),tenant.getContinents());
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

	@PostMapping("/logout")
	@ResponseBody
	public Object logout(@RequestBody String refreshToken){
		return accessTokenService.logout(refreshToken);
	}

	/**
	 * Function that returns user from userRepository
	 * @param id - id of the wanted user
	 * @return User represents the wanted user, null if user not exist.
	 */
	private User getUserFromRepository(long id) {
		Optional<User> userExists = userRepository.findById(id);
		return userExists.orElse(null);
	}

	/**
	 * Function that returns user from userRepository
	 * @param username - username of the wanted user
	 * @return User represents the wanted user, null if user not exist.
	 */
	private User getUserFromRepositoryByUsername(String username) {
		for(User user:userRepository.findAll()){
			if(user.getUserName().equals(username))
				return getUserFromRepository(user.getId());
		}
		return null;
	}


	/**
	 * Function that returns tenant from tenantRepository
	 * @param id - id of the wanted tenant
	 * @return Tenant represents the wanted tenant, null if tenant not exist.
	 */
	private Tenant getTenantFromRepository(long id) {
		Optional<Tenant> tenantExists = tenantRepository.findById(id);
		return tenantExists.orElse(null);
	}

	/**
	 * Function that returns role from RoleRepository
	 * @param id - id of the wanted role
	 * @return Role represents the wanted role, null if role not exist.
	 */
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


	/**
	 * This function checks if the logged in user is from the same tenant to enforce requests authorization such as DELETE, PUT, POST
	 * @param user
	 * @return
	 */
	private boolean isTokenUserFromSameTenant(User user){
		User tokenUser=getUserFromToken(token.getPreferredUsername());
		if(tokenUser==null)
			return false;
		Role tokenRole=getRoleFromRepository(tokenUser.getRoleID());
		if(tokenRole==null)
			return false;
		if(tokenRole.getName().equals("Admin"))
			return true;
		if(tokenRole.getName().equals("User"))
			return false;
		if(tokenRole.getName().equals("Region-Admin")){
			if(!tokenUser.getTenantID().containsAll(user.getTenantID())){
				return false;
			}
		}
		return true;
	}

}