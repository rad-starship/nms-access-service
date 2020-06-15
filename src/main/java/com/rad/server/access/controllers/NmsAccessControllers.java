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

import javax.management.BadAttributeValueExpException;
import javax.management.InstanceAlreadyExistsException;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
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

	@Autowired
	private HashSet<String> tokenBlackList;



	@GetMapping("/users")
	@ResponseBody
	public Object getUsers(@RequestHeader HttpHeaders headers)
	{
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
	public Object getUserToken(@RequestHeader HttpHeaders headers,@PathVariable String username)
	{
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		User tokenUser=getUserFromToken(username);
		ArrayList<String> tenants=new ArrayList<>();
		for(long id:tokenUser.getTenantID()){
			for(String continent:getTenantFromRepository(id).getContinents()){
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
	public Object addUser(@RequestBody User user,@RequestHeader HttpHeaders headers)
	{
		try {
			if(tokenBlackList.contains(headers.get("Authorization").get(0)))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
					int response=userService.addKeycloakUser(user,realms,role.getName());
					if(response==409)
						throw new Exception();
					if(response==400)
						throw new BadRequestException();
					user.encodePassword(user.getPassword());
					userRepository.save(user);
					return user;
				}
				else throw new NotFoundException();

		}
		catch (NotFoundException e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Tenant ID or Role ID Does not exists").getHttpResponse();
		}
		catch (InstanceAlreadyExistsException e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Username already exists").getHttpResponse();
		}
		catch (java.lang.Error e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
		}
		catch (BadRequestException e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Password doesnt meet the requirements").getHttpResponse();
		}
		catch (Exception e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Email already exists in the system").getHttpResponse();
		}
	}

	/**
	 * This function deletes a user from the repository and from keycloak by id if it exists
	 * @param id
	 * @return
	 */

	@DeleteMapping("/users/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteUser(@PathVariable long id,@RequestHeader HttpHeaders headers) {
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		User user;
		user=getUserFromRepository(id);
		if(user==null){
			System.out.println("The user doesnt exist.");
			return new HttpResponse(HttpStatus.BAD_REQUEST,"user Doesnt Exist").getHttpResponse();
		}
		if(!isTokenUserFromSameTenant(user))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
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
				userRepository.delete(user);
				System.out.println("User deleted successfully.");
				ResponseEntity<User> result = new ResponseEntity<>(user, HttpStatus.ACCEPTED);
				return new HttpResponse(result).getHttpResponse();

	}



	/**
	 * This function updates a registered user by id if it exists
	 * @param id
	 * @param user The new user details
	 * @return
	 */
	@PutMapping("/users/{id}")

	public ResponseEntity<?> updateUser(@PathVariable long id,@RequestBody User user,@RequestHeader HttpHeaders headers){
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		User oldUser=getUserFromRepository(id);
		if(!isTokenUserFromSameTenant(oldUser))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
		if(oldUser==null)
			return new HttpResponse(HttpStatus.BAD_REQUEST,"User doesnt exists").getHttpResponse();
		ArrayList<String> realms=new ArrayList<>();
		for (Long tenant:oldUser.getTenantID()) {
			if(tenantRepository.existsById(tenant)){
				realms.add(tenantRepository.findById(tenant).get().getName());
			}
		}
		for(String realm:realms) {
			User newUser = new User(user);
			newUser.setId(id);
			newUser.setUserName(oldUser.getUserName());
			boolean result=userService.updateKeycloakUser(user, oldUser.getUserName(),user.getPassword(),realm);
			if(!result)
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Password doesnt meet the requirements").getHttpResponse();
			newUser.encodePassword(user.getPassword());
			newUser.setRoleID(oldUser.getRoleID());
			newUser.setTenantsID(oldUser.getTenantID());
			userRepository.save(newUser);
		}
		ResponseEntity<User> result = new ResponseEntity<>(user,HttpStatus.ACCEPTED);
		return new HttpResponse(result).getHttpResponse();
	}
	
	@GetMapping("/roles")
	@ResponseBody
	public Object getRoles(@RequestHeader HttpHeaders headers)
	{
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
	public Object addRole(@RequestBody Role role,@RequestHeader HttpHeaders headers)
	{
	    try {
			if(tokenBlackList.contains(headers.get("Authorization").get(0)))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
            System.out.println("addRole: " + role);
            //roleRepository.save(role);
            roleService.addRole(role);
            return role;
        }
	    catch (Exception e){
	        return  new HttpResponse(HttpStatus.CONFLICT,"Cannot add role").getHttpResponse();
        }
	}

	/**
	 * This function updates an existing role by id if it exists
	 * @param roleId
	 * @param roleDetails The new role details
	 * @return
	 */
	@PutMapping("/roles/{id}")
	@ResponseBody
	public Object updateRole(@PathVariable(value = "id") Long roleId,
												   @Valid @RequestBody Role roleDetails,@RequestHeader HttpHeaders headers) {
		try {
			if(tokenBlackList.contains(headers.get("Authorization").get(0)))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
	public ResponseEntity<?> deleteRole(@PathVariable(value = "name") String roleName,@RequestHeader HttpHeaders headers){

	System.out.println("DeleteRole: " + roleName);
	try {
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		if(roleName.equals("Admin")){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
		}
			roleService.deleteRole(new Role(roleName));
			Map<String,String> result= new HashMap<>();
			result.put("Result","Success");
			return new ResponseEntity<>(result,HttpStatus.ACCEPTED);
	}
	catch(NotFoundException e){
		return new  ResponseEntity<>("Not found",HttpStatus.NO_CONTENT);
	}



}
	/**
	 * This function deletes a role from the repository and the keycloak servers by role ID
	 * @param roleId
	 * @return
	 */
	@DeleteMapping("/rolesid/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteRole(@PathVariable(value = "id") long roleId,@RequestHeader HttpHeaders headers){
		System.out.println("DeleteRole: " + roleId);
		try {
			if(tokenBlackList.contains(headers.get("Authorization").get(0)))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
			if(roleRepository.existsById(roleId)){
				if(roleRepository.findById(roleId).get().getName().equals("Admin"))
					return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
			}
			roleService.deleteRole(roleId);
			Map<String,Long> values = new HashMap<>();
			values.put("RoleId",roleId);
			ResponseEntity<Object> result = new ResponseEntity<>(values,HttpStatus.ACCEPTED);
			return new HttpResponse(result).getHttpResponse();
		}
		catch(NotFoundException e){
			ResponseEntity<Long> result = new ResponseEntity<>(roleId,HttpStatus.NO_CONTENT);
			return new HttpResponse(result).getHttpResponse();
		}


	}


	@GetMapping("/permissions")
	@ResponseBody
	public Object getPermissions(@RequestHeader HttpHeaders headers){
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		List<Map<String,String>> permissions = roleService.getPermissions();
		return permissions;
	}


	@GetMapping("/tenants")
	@ResponseBody
	public Object getTenants(@RequestHeader HttpHeaders headers)
	{
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
	public Object addTenant(@RequestBody Tenant tenant,@RequestHeader HttpHeaders headers)
	{
		try {
			if(tokenBlackList.contains(headers.get("Authorization").get(0)))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
			System.out.println("addTenant: " + tenant);
			tenantRepository.save(tenant);
			tenantService.addKeycloakTenant(tenant);
			return tenant;
		}
		catch (Exception e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Tenant already exists").getHttpResponse();
		}
	}


	/**
	 * This function deletes a tenant from the repository and the keycloak servers by ID, if it exists
	 * @param id
	 * @return
	 */
	@DeleteMapping("/tenants/{id}")
	@ResponseBody
	public Object deleteTenant(@PathVariable long id,@RequestHeader HttpHeaders headers){
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
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
	public Object updateTenant(@PathVariable long id,@RequestBody Tenant tenant,@RequestHeader HttpHeaders headers){
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
		Tenant oldTenant=getTenantFromRepository(id);
		if(oldTenant==null) {
			return new HttpResponse(HttpStatus.BAD_REQUEST,"The tenant does not exist").getHttpResponse();
		}
		Tenant newTenant=new Tenant(tenant.getName(),tenant.getContinents());
		newTenant.setId(id);
		tenantService.updateKeycloakTenant(tenant,oldTenant.getName());
		tenantRepository.save(newTenant);
		return tenant;
	}

	@PostMapping("/settings")
    @ResponseBody
    public Object postSettings(@RequestBody Object settings,@RequestHeader HttpHeaders headers){
		if(tokenBlackList.contains(headers.get("Authorization").get(0)))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"You need to login first").getHttpResponse();
        System.out.println(settings);

		Settings settings1 = settingsService.parseSettings(settings);
		settingsService.applySettings(settings1);

		return settings;

    }

    @PostMapping("/getToken")
	@ResponseBody
	public Object login(@RequestBody(required = false) LoginEntity loginEntity){
		System.out.println("Login Request: User=" + loginEntity.getUsername() + ", Tenant:" + loginEntity.getTenant());
		return accessTokenService.getAccessToken(loginEntity);
	}

    static class LogoutRequest
    {
    	String refreshToken;
    	public LogoutRequest()
    	{
    		
    	}
		public String getRefreshToken()
		{
			return this.refreshToken;
		}
		public void setRefreshToken(String refreshToken)
		{
			this.refreshToken = refreshToken;
		}
    }
    
	@PostMapping("/logout")
	@ResponseBody
	public Object logout(@RequestBody LogoutRequest logoutRequest,@RequestHeader HttpHeaders headers){
		tokenBlackList.add(headers.get("Authorization").get(0));
		return accessTokenService.logout(logoutRequest.refreshToken);
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
			if(user.getUserName().toLowerCase().equals(username.toLowerCase()))
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