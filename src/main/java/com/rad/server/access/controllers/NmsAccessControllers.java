package com.rad.server.access.controllers;

import java.util.*;

import com.rad.server.access.componenets.KeycloakAdminProperties;
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
	private RoleRepository	roleRepository;

    @Autowired
    private Settings settings;

    //***********************************************************************
    //                          Users APIs
    //***********************************************************************


	@GetMapping("/users")
	@ResponseBody
	public Object getUsers(@RequestHeader HttpHeaders headers)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.getUsers();
	}

	@GetMapping("/users/{id}")
	@ResponseBody
	public Object getUsers(@RequestHeader HttpHeaders headers,@PathVariable long id)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.getUser(id);
	}

	/**
	 * This function returns the continent list that the logged in user is allowed to watch in health service
	 * @param headers
	 * @param username
	 * @return
	 */
	@GetMapping("/users/getContinentsByToken/{username}")
	@ResponseBody
	public Object getContinentsByToken(@RequestHeader HttpHeaders headers,@PathVariable String username)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.getContinentsByToken(username);
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
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.addUser(user);
	}

	/**
	 * This function deletes a user from the repository and from keycloak by id if it exists
	 * @param id
	 * @return
	 */
	@DeleteMapping("/users/{id}")
	@ResponseBody
	public ResponseEntity<?> deleteUser(@PathVariable long id,@RequestHeader HttpHeaders headers) {
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.deleteKeycloakUser(id);
	}

	/**
	 * This function updates a registered user by id if it exists
	 * @param id
	 * @param user The new user details
	 * @return
	 */
	@PutMapping("/users/{id}")
	public ResponseEntity<?> updateUser(@PathVariable long id,@RequestBody User user,@RequestHeader HttpHeaders headers){
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return userService.updateUser(id,user);
	}

    //***********************************************************************
    //                          Roles APIs
    //***********************************************************************

	@GetMapping("/roles")
	@ResponseBody
	public Object getRoles(@RequestHeader HttpHeaders headers)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
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
			if(accessTokenService.isInBlackList(headers))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
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
			if(accessTokenService.isInBlackList(headers))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
			return roleService.updateRole(roleId, roleDetails);
		}
			catch(Exception e){
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Role doesnt exists").getHttpResponse();
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
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
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
			if(accessTokenService.isInBlackList(headers))
				return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
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
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		List<Map<String,String>> permissions = roleService.getPermissions();
		return permissions;
	}

    //***********************************************************************
    //                          Tenants APIs
    //***********************************************************************
	@GetMapping("/tenants")
	@ResponseBody
	public Object getTenants(@RequestHeader HttpHeaders headers)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return tenantService.getTenants();
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
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return tenantService.addKeycloakTenant(tenant);

	}


	/**
	 * This function deletes a tenant from the repository and the keycloak servers by ID, if it exists
	 * @param id
	 * @return
	 */
	@DeleteMapping("/tenants/{id}")
	@ResponseBody
	public Object deleteTenant(@PathVariable long id,@RequestHeader HttpHeaders headers){
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return tenantService.deleteKeycloakTenant(id);
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
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return tenantService.updateKeycloakTenant(tenant,id);
	}


    //***********************************************************************
    //                          Settings APIs
    //***********************************************************************

	@GetMapping("/settings")
	@ResponseBody
	public Object getSettings(@RequestHeader HttpHeaders headers)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return settings;
	}

	@GetMapping("/settings/isOnline")
	@ResponseBody
	public Object isOnline(@RequestHeader HttpHeaders headers)
	{
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return settings.isOnline();
	}


	@PostMapping("/settings")
    @ResponseBody
    public Object postSettings(@RequestBody Object settings,@RequestHeader HttpHeaders headers){
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		try {
			System.out.println(settings);

			Settings settings1 = settingsService.parseSettings(settings);
			this.settings=settings1;
			System.out.println(settings1.toJson());
			settingsService.applySettings(settings1);

			settingsService.updateES(settings1);

			return settings;
		}
		catch(Exception e){
			return new HttpResponse(HttpStatus.BAD_REQUEST,e.getMessage()).getHttpResponse();
		}

    }


    //***********************************************************************
    //                          Management APIs
    //***********************************************************************

    @PostMapping("/getToken")
	@ResponseBody
	public Object login(@RequestBody(required = false) LoginEntity loginEntity){
		System.out.println("Login Request: User=" + loginEntity.getUsername() + ", Tenant:" + loginEntity.getTenant());
		return accessTokenService.login(loginEntity);
	}

    static class LogoutRequest
    {
    	String refresh;
    	public LogoutRequest()
    	{
    		
    	}

		public String getRefresh() {
			return refresh;
		}

		public void setRefresh(String refresh) {
			this.refresh = refresh;
		}
	}
    
	@PostMapping("/logout")
	@ResponseBody
	public Object logout(@RequestBody LogoutRequest logoutRequest,@RequestHeader HttpHeaders headers){
		accessTokenService.addToBlackList(headers.get("Authorization").get(0));
		return accessTokenService.logout(logoutRequest.getRefresh());
	}

	@GetMapping("/sessions")
	@ResponseBody
	public Object getSessions(@RequestHeader HttpHeaders headers){
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return accessTokenService.getSessions();
	}

	@GetMapping("/events")
	@ResponseBody
	public Object getEvents(@RequestHeader HttpHeaders headers){
		if(accessTokenService.isInBlackList(headers))
			return new HttpResponse(HttpStatus.BAD_REQUEST,"Token is inactive").getHttpResponse();
		return accessTokenService.getEvents();
	}




}