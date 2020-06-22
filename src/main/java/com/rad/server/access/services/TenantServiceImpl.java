package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Role;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.User;
import com.rad.server.access.entities.settings.PasswordPolicy;
import com.rad.server.access.entities.settings.Settings;
import com.rad.server.access.entities.settings.Token;
import com.rad.server.access.entities.settings.otpPolicy;
import com.rad.server.access.repositories.RoleRepository;
import com.rad.server.access.repositories.TenantRepository;
import com.rad.server.access.repositories.UserRepository;
import com.rad.server.access.responses.HttpResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.HttpHostConnectException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.ws.rs.ProcessingException;
import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantServiceImpl implements TenantService {

    @Autowired
    private KeycloakAdminProperties prop;

    @Autowired
    private  SettingsService settingsService;

    @Autowired
    private Settings settings;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private AccessToken token;

    private Map<String,ClientRepresentation> clientRepresentationMap = new ConcurrentHashMap<>();
    final String webUri = "http://localhost:4200/*";
    final String serverUri = "http://localhost:8083/*";
    final String dataUri = "http://localhost:8082/*";
    final String nmsUri = "http://localhost:8081/*";

    /**
     * This function returns the keycloak server
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

    public Object getTenants(){
        List<Tenant> tenants = (List<Tenant>) tenantRepository.findAll();
        System.out.println("getTenants: " + tenants);
        return tenants;
    }


    /**
     * This function creates a new tenant(realm) in the keycloak server and configures it to match the current settings
     * @param tenant
     */
    @Override
    public Object addKeycloakTenant(Tenant tenant) {
        try {

            System.out.println("addTenant: " + tenant);
            tenantRepository.save(tenant);

            Keycloak keycloak = getKeycloakInstance();
            Token token = settings.getAuthentication().getToken();
            otpPolicy otpPolicy = settings.getAuthentication().getOtpPolicy();
            RealmRepresentation realm = new RealmRepresentation();
            realm.setRealm(tenant.getName());
            realm.setEnabled(true);
            settingsService.applyTokenToRealm(token, realm);
            keycloak.realms().create(realm);
            addAllClients(tenant.getName());
            if (otpPolicy.isEnabled())
                setOTP(tenant.getName());

            return tenant;
        }
        catch (Exception e){
            return new HttpResponse(HttpStatus.BAD_REQUEST,"Tenant already exists").getHttpResponse();
        }
    }

    @Override
    public void addIdentityProvider(String providerID,String secret,String clientID,String realm){
        Keycloak keycloak=getKeycloakInstance();
        IdentityProviderRepresentation identityProviderRepresentation=new IdentityProviderRepresentation();
        Map<String,String> config=new HashMap<>();
        config.put("clientSecret",secret);
        config.put("clientId",clientID);
        config.put("useJwkUrl","true");
        identityProviderRepresentation.setConfig(config);
        identityProviderRepresentation.setAlias(providerID);
        identityProviderRepresentation.setProviderId(providerID);
        identityProviderRepresentation.setStoreToken(true);
        identityProviderRepresentation.setEnabled(true);
        keycloak.realm(realm).identityProviders().create(identityProviderRepresentation);
    }


    /**
     * This function sets OTP authentication for the keycloak tenant
     * @param realm
     */
    private void setOTP(String realm){
        Keycloak keycloak=getKeycloakInstance();
        List<RequiredActionProviderRepresentation> requiredActionProviderRepresentations;
        requiredActionProviderRepresentations=keycloak.realm(realm).flows().getRequiredActions();
        for(RequiredActionProviderRepresentation r:requiredActionProviderRepresentations){
            if(r.getName().equals("Configure OTP")){
                r.setEnabled(true);
                r.setDefaultAction(true);
                keycloak.realm(realm).flows().updateRequiredAction(r.getAlias(),r);
                break;
            }
        }
    }


    /**
     * This function adds all of the needed clients to the keycloak tenant
     * @param realm
     */
    private void addAllClients(String realm) {
        //CORONA-WEB-CLIENT
        ProtocolMapperRepresentation protocolMapperRepresentation=new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("details");
        protocolMapperRepresentation.setProtocol("openid-connect");
        protocolMapperRepresentation.setProtocolMapper("User Attribute");
        ClientRepresentation web = getClientRep("corona-web",webUri);
        ClientRepresentation server = getClientRep("corona-server",serverUri);
        ClientRepresentation nms = getClientRep("corona-nms",nmsUri);
        ClientRepresentation health = getClientRep("health-data",dataUri);
        ClientsResource clients = getKeycloakInstance().realm(realm).clients();

        try{

            clients.create(web);
            clients.create(server);
            clients.create(nms);
            clients.create(health);
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Client already exists");
        }
    }

    private ClientRepresentation getClientRep(String name,String uri){
        if(clientRepresentationMap.get(name)!= null){
            return clientRepresentationMap.get(name);
        }
        else
        {
        ClientRepresentation output = new ClientRepresentation();
        output.setClientId(name);
        List<String> urls = new ArrayList<>();
        urls.add(uri);
        output.setRedirectUris(urls);
        output.setPublicClient(true);
        output.setDirectAccessGrantsEnabled(true);
        clientRepresentationMap.put(name,output);
        return output;}
    }

    /**
     * This function removes the keycloak tenant by name
     * @param id of the realm to delete
     */
    @Override
    public Object deleteKeycloakTenant(long id) {
        Tenant tenant;
        tenant=getTenantFromRepository(id);
        if(tenant!=null) {
            if(tenant.getName().equals("Admin"))
                return new HttpResponse(HttpStatus.BAD_REQUEST,"cannot delete Admin").getHttpResponse();
            User tokenUser=getUserFromToken(token.getPreferredUsername());
            if(!getRoleFromRepository(tokenUser.getRoleID()).getPermissions().contains("all")){
                return new HttpResponse(HttpStatus.BAD_REQUEST,"Keycloak user unauthorized").getHttpResponse();
            }
            tenantRepository.delete(tenant);
            Keycloak keycloak=getKeycloakInstance();
            keycloak.realms().realm(tenant.getName()).remove();
            ResponseEntity<Tenant> result = new ResponseEntity<Tenant>(tenant,HttpStatus.ACCEPTED);
            return result;
        }
        else
            return new HttpResponse(HttpStatus.BAD_REQUEST,"wrong tenant id");

    }

    /**
     * This function updates an existing keycloak tenant to match the new tenant
     * @param tenant The new tenant
     * @param id of the tenant to update
     */
    @Override
    public Object updateKeycloakTenant(Tenant tenant,long id) {

        Tenant oldTenant=getTenantFromRepository(id);
        if(oldTenant==null) {
            return new HttpResponse(HttpStatus.BAD_REQUEST,"The tenant does not exist").getHttpResponse();
        }
        Tenant newTenant=new Tenant(tenant.getName(),tenant.getContinents());
        newTenant.setId(id);

        Keycloak keycloak=getKeycloakInstance();
        RealmRepresentation realm=new RealmRepresentation();
        realm.setRealm(tenant.getName());
        keycloak.realms().realm(oldTenant.getName()).update(realm);

        tenantRepository.save(newTenant);
        return tenant;

    }

    /**
     * This function checks if the tenant exists in the keycloak servers
     * @param tenant
     * @return
     */
    @Override
    public boolean tenantExists(Tenant tenant){
        try {
            Keycloak keycloak = getKeycloakInstance();


            RealmsResource realms = keycloak.realms();
            List<RealmRepresentation> existingRealms = realms.findAll();
            for (RealmRepresentation r : existingRealms) {
                if (r.getRealm().equals(tenant.getName()))
                    return true;
            }
            return false;
        }
        catch(ProcessingException e){
            System.out.println("Unable to connect KC");
            return false;
        }
    }

    /**
     * This function initializes the keycloak tenants from repository and the updates the repository if there are
     * tenants in keycloak that doesnt exist in the repository
     * @param repository
     */
    public void initKeycloakTenants(TenantRepository repository){
        try {
            Keycloak keycloak = getKeycloakInstance();
            for (Tenant t : repository.findAll()) {
                if (!tenantExists(t)) {
                    addKeycloakTenant(t);
                    addIdentityProvider("google", "lMfvuhAZjvm66wLQEyNtUY1Q", "604757352616-tvcgf7nefknumcgoo27q8c5bmeie0sb9.apps.googleusercontent.com\n", t.getName());
                }
            }
            for (RealmRepresentation r : keycloak.realms().findAll()) {
                if (r.getRealm().equals("master"))
                    continue;
                //Add Clients for each  KC relm
                addAllClients(r.getRealm());

                boolean exists = false;
                for (Tenant t : repository.findAll()) {
                    if (t.getName().equals(r.getRealm()))
                        exists = true;
                }
                if (exists)
                    continue;
                else {
                    Tenant newTenant = new Tenant(r.getRealm());
                    repository.save(newTenant);
                }
            }
        }
        catch(Exception e){
            System.out.println("Cannot init tenants KC is offline..");
            return;
        }
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




}
