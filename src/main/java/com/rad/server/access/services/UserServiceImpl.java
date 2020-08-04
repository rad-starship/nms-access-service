package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.repositories.RoleRepository;
import com.rad.server.access.repositories.TenantRepository;
import com.rad.server.access.repositories.UserRepository;
import com.rad.server.access.responses.HttpResponse;
import org.apache.commons.codec.binary.Base64;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.AccessToken;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.management.InstanceAlreadyExistsException;
import javax.websocket.OnClose;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    AccessToken token;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired UserRepository userRepository;

    @Autowired
    AccessTokenService accessTokenService;


    /**
     * This function returns a keycloak instance to work with and manipulate
     * @return
     */
    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }


    /**
     * This function returns all the users
     * @return
     */

    public List<User> getUsers(){
        List<User> users =(List<User>) userRepository.findAll();
        System.out.println("getUsers: " + users);
        return users;
    }

    /**
     * This function gets user by id
     * @param id id of the user to get
     * @return
     */

    public Object getUser(long id){
        User user=getUserFromRepository(id);
        if(user==null)
            return new HttpResponse(HttpStatus.BAD_REQUEST,"User doesnt exist").getHttpResponse();
        return user;
    }

    /**
     * This function adds a new user to keycloak and the repository
     * @param user the new user to add
     * @return
     */

    public Object addUser(User user) {
        try {
            if (!isTokenUserFromSameTenant(user)) {
                throw new Error();
            }
            ArrayList<String> realms = new ArrayList<>();
            for (Long tenant : user.getTenantsID()) {
                if (tenantRepository.existsById(tenant)) {
                    realms.add(tenantRepository.findById(tenant).get().getName());
                } else {
                    throw new NotFoundException();
                }
            }
            if (roleRepository.existsById(user.getRoleID())) {
                Role role = roleRepository.findById(user.getRoleID()).get();
                User exists = getUserFromRepositoryByUsername(user.getUserName());
                if (exists != null) {
                    if (exists.getTenantsID().containsAll(user.getTenantsID()))
                        throw new InstanceAlreadyExistsException();
                    else {
                        user.setId(exists.getId());
                        for (long id : exists.getTenantsID()) {
                            if (!user.getTenantsID().contains(id))
                                user.getTenantsID().add(id);
                        }
                    }
                }
                int response = addKeycloakUser(user, realms, role.getName());
                if (response == 409)
                    throw new Exception();
                if (response == 400)
                    throw new BadRequestException();
                user.encodePassword(user.getPassword());
                userRepository.save(user);
                return user;
            } else throw new NotFoundException();

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
     * This function updates an existing user in the keycloak and in the repository
     * @param id id of the user to update
     * @param user the user new details
     * @return
     */

    public ResponseEntity<?> updateUser(long id,User user){
        User oldUser=getUserFromRepository(id);
        if(!isTokenUserFromSameTenant(oldUser))
            return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
        if(oldUser==null)
            return new HttpResponse(HttpStatus.BAD_REQUEST,"User doesnt exists").getHttpResponse();
        ArrayList<String> realms=new ArrayList<>();
        for (Long tenant:oldUser.getTenantsID()) {
            if(tenantRepository.existsById(tenant)){
                realms.add(tenantRepository.findById(tenant).get().getName());
            }
        }
        for(String realm:realms) {
            User newUser = new User(user);
            newUser.setId(id);
            newUser.setUserName(oldUser.getUserName());
            boolean result=updateKeycloakUser(user, oldUser.getUserName(),user.getPassword(),realm);
            if(!result)
                return new HttpResponse(HttpStatus.BAD_REQUEST,"Password doesnt meet the requirements").getHttpResponse();
            newUser.encodePassword(user.getPassword());
            newUser.setRoleID(oldUser.getRoleID());
            newUser.setTenantsID(oldUser.getTenantsID());
            userRepository.save(newUser);
        }
        ResponseEntity<User> result = new ResponseEntity<>(user,HttpStatus.ACCEPTED);
        return new HttpResponse(result).getHttpResponse();
    }

    /**
     * This function returns all the continent of a user
     * @param username the username of the wanted user
     * @return
     */
    public Object getContinentsByToken(String username){
        User tokenUser=accessTokenService.getUserFromToken(username);
        ArrayList<String> tenants=new ArrayList<>();
        for(long id:tokenUser.getTenantsID()){
            for(String continent:getTenantFromRepository(id).getContinents()){
                if(!tenants.contains(continent))
                    tenants.add(continent);
            }
        }
        return tenants;
    }


    /**
     * This function deletes the user from the keycloak servers by username and realm
     * @param id of the deleted username
     */

    @Override
    public ResponseEntity<?> deleteKeycloakUser(long id){

        User user;
        user=getUserFromRepository(id);
        if(user==null){
            System.out.println("The user doesnt exist.");
            return new HttpResponse(HttpStatus.BAD_REQUEST,"user Doesnt Exist").getHttpResponse();
        }
        if(!isTokenUserFromSameTenant(user))
            return new HttpResponse(HttpStatus.BAD_REQUEST,"keycloak user not authorized").getHttpResponse();
        if(user.getUserName().equals("admin"))
            return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
        for (Long tenants: user.getTenantsID()) {
            Tenant tenant=getTenantFromRepository(tenants);
            if(tenant==null)
                return new HttpResponse(HttpStatus.BAD_REQUEST,"tenant is null").getHttpResponse();
            Keycloak keycloak=getKeycloakInstance();
            RealmResource realmResource = keycloak.realm(tenant.getName());
            UsersResource users =  realmResource.users();
            users.delete(users.search(user.getUserName()).get(0).getId());
        }
        userRepository.delete(user);
        System.out.println("User deleted successfully.");
        ResponseEntity<User> result = new ResponseEntity<>(user, HttpStatus.ACCEPTED);
        return new HttpResponse(result).getHttpResponse();
    }


    /**
     * This function adds a new user to the keycloak serve, adds his permissions,
     * and applies the necessary settings
     * @param user
     * @param tenants
     * @param role
     */
    @Override
    public int addKeycloakUser(User user,ArrayList<String> tenants,String role) {
        Keycloak keycloak = getKeycloakInstance();

        for (String tenant:tenants) {
            List<CredentialRepresentation> credentials=new ArrayList<>();
            UserRepresentation userRep= new UserRepresentation();
            userRep.setEnabled(true);
            userRep.setUsername(user.getUserName());
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            userRep.setEmail(user.getEmail());
            userRep.singleAttribute("Username",user.getUserName());
            userRep.singleAttribute("role",role);
            userRep.singleAttribute("realm",tenant);
            CredentialRepresentation credentialRepresentation=new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(user.getPassword());
            credentialRepresentation.setTemporary(false);
            credentials.add(credentialRepresentation);
            userRep.setCredentials(credentials);
            RealmResource realmResource = keycloak.realm(tenant);
            UsersResource usersResource = realmResource.users();
            Response response=usersResource.create(userRep);
            if(response.getStatus()==409||response.getStatus()==400)
                return response.getStatus();
            if(response.getStatus()!=400) {
                UserRepresentation addUserRole = usersResource.search(user.getUserName()).get(0);
                UserResource updateUser = usersResource.get(addUserRole.getId());
                List<RoleRepresentation> roleRepresentationList = updateUser.roles().realmLevel().listAvailable();

                for (RoleRepresentation roleRepresentation : roleRepresentationList) {
                    if (roleRepresentation.getName().equals(role)) {
                        updateUser.roles().realmLevel().add(Arrays.asList(roleRepresentation));
                        break;
                    }
                }
            }
            System.out.printf("Repsonse: %s %s%n", response.getStatus(), response.getStatusInfo());
        }
        return 0;
    }

    /**
     * This function updates an existing user in the keycloak server by username
     * @param user
     * @param userName
     */
    public boolean updateKeycloakUser(User user ,String userName,String password,String realm) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            List<CredentialRepresentation> credentials = new ArrayList<>();
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource users = realmResource.users();
            UserRepresentation userRep = users.search(userName).get(0);
            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(password);
            credentialRepresentation.setTemporary(false);
            credentials.add(credentialRepresentation);
            UserResource updateUser = users.get(userRep.getId());
            updateUser.resetPassword(credentialRepresentation);
            userRep.setEmail(user.getEmail());
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            updateUser.update(userRep);
            return true;
        }
        catch (Exception e){
            return false;
        }

    }

    /**
     * This function initializes the keycloak server users and also initializes the repository
     * with new users that are in the keycloak server
     * @param userRepository
     */
    public void initKeycloakUsers(UserRepository userRepository){
        Keycloak keycloak=getKeycloakInstance();
        RealmsResource realms=keycloak.realms();
        List<RealmRepresentation> existingRealms=realms.findAll();

        for(RealmRepresentation r:existingRealms){
            if(r.getRealm().equals("master"))
                continue;
            UsersResource users=keycloak.realm(r.getRealm()).users();
            for(UserRepresentation u:users.list()){
                if(!userExistsInRepository(u.getUsername(),r.getRealm(),userRepository)) {
                    ArrayList<Long> realmsToAdd=new ArrayList<>();
                    realmsToAdd.add(getTenantID(r.getRealm()));
                    User addUser=new User(u.getFirstName(),u.getLastName(),u.getEmail(),u.getUsername(),"",getRoleID(keycloak.realm(r.getRealm()).users().get(u.getId()).roles().realmLevel().listEffective()),realmsToAdd);
                    userRepository.save(addUser);
                }
            }
        }

    }

    /**
     * This function checks if the user exists in the user repository by username and tenant
     * @param username
     * @param realm
     * @param userRepository
     * @return
     */
    public boolean userExistsInRepository(String username,String realm,UserRepository userRepository){
        for(User user:userRepository.findAll()){
            if(user.getUserName().toLowerCase().equals(username)){
                long tenantID=getTenantID(realm);
                for(long id:user.getTenantsID()){
                    if(id==tenantID)
                        return true;
                }
                user.addTenant(tenantID);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private long getTenantID(String tenant){
        for(Tenant t:tenantRepository.findAll()){
            if(t.getName().equals(tenant))
               return t.getId();
        }
        return 0;
    }

    private long getRoleID(List<RoleRepresentation> roles){
        for(RoleRepresentation role:roles){
            for(Role r:roleRepository.findAll()){
                if(r.getName().equals(role.getName()))
                   return r.getId();
            }
        }

        return 0;
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
     * This function checks if the logged in user is from the same tenant to enforce requests authorization such as DELETE, PUT, POST
     * @param user
     * @return
     */
    private boolean isTokenUserFromSameTenant(User user){
        User tokenUser=accessTokenService.getUserFromToken(token.getPreferredUsername());
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
            if(!tokenUser.getTenantsID().containsAll(user.getTenantsID())){
                return false;
            }
        }
        return true;
    }

}