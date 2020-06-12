package com.rad.server.access;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rad.server.access.adapters.MultitenantConfiguration;
import com.rad.server.access.entities.settings.*;
import com.rad.server.access.services.SettingsService;
import com.rad.server.access.services.TenantService;

import com.rad.server.access.services.RoleService;
import com.rad.server.access.services.UserService;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.events.Event;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.entities.Role;
import com.rad.server.access.repositories.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@SpringBootApplication
@ImportAutoConfiguration(MultitenantConfiguration.class)
public class NmsAccessApplication implements ApplicationListener<ApplicationReadyEvent>
{

	private static final Logger LOG = LoggerFactory.getLogger("KafkaApp");

	private final KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	public NmsAccessApplication(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
	private SettingsService settingsService;

    private Settings initSettings;
	
	public static void main(String[] args)
	{
		SpringApplication.run(NmsAccessApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event)
	{
		try
		{
			String ip       = InetAddress.getLocalHost().getHostAddress();
			String hostName = InetAddress.getLocalHost().getHostName();
			int port        = applicationContext.getBean(Environment.class).getProperty("server.port", Integer.class, 8080);
			
			System.out.println("*****************************************************");
			System.out.println("* NMS Access Service is Ready ");
			System.out.println("* Host=" + hostName + ", IP=" + ip + ", Port=" + port);
			System.out.println("*****************************************************");
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
	}

	@KafkaListener(topics = "events", groupId = "rad")
	public void listen(String message) {
		LOG.info("Received message in rad group: {}", message);
	}


	@Bean
	HashSet<String> tokenBlackListInit(){
		return new HashSet<>();
	}

	@Bean
	Settings settingsInit(){
			Autorization autorization=new Autorization();
			Token token=new Token(30,600,1500,20);
			PasswordPolicy passwordPolicy=new PasswordPolicy(365,8,3,1,true);
			otpPolicy otpPolicy=new otpPolicy(false,"Time Based",8,30);
			SocialLogin socialLogin=new SocialLogin("None");
			Authentication authentication=new Authentication(token,passwordPolicy,otpPolicy,socialLogin);
			initSettings=new Settings(authentication,autorization,true);
			return initSettings;
	}

	/**
	 * Populate the database with a few User entities
	 */
	@Bean
	CommandLineRunner tenantInit(TenantRepository repository, TenantService tenantService)
	{
		return args -> {
			Stream.of("Admin", "America", "Europe", "Asia", "Africa","All").forEach(name -> {
				ArrayList<String> continents=new ArrayList<>();
				continents.add(name);
				Tenant t = new Tenant(name,continents);
				repository.save(t);
			});
			repository.findAll().forEach(System.out::println);
			tenantService.initKeycloakTenants(repository);
		};
	}


	/**
	 * Init all roles of the system. first init the permissions which are not composite roles and then init the actual roles.
	 */
	@Bean
	CommandLineRunner roleInit(RoleRepository repository, RoleService roleService)
	{
		return args -> {
       //First init the permissions

            Stream.of("all","user_write","user_read","role_write","role_read","tenant_write","tenant_read","corona_read").forEach(name -> {
                Role t = new Role(name);
                roleService.initRole(t);
            });// to talk to amir about that
            //Now create the default roles
			Role admin = new Role("Admin");
			List<String> adminPermission = new LinkedList<>();
			adminPermission.add("all");
			admin.addPermission(adminPermission);
			roleService.initRole(admin);

            Role regionAdmin = new Role("Region-Admin");
            List<String> rAdminPermission = new LinkedList<>();
            rAdminPermission.add("user_write");
            rAdminPermission.add("user_read");
            rAdminPermission.add("role_write");
            rAdminPermission.add("role_read");
            rAdminPermission.add("tenant_read");
            rAdminPermission.add("corona_read");
            regionAdmin.addPermission(rAdminPermission);
            roleService.initRole(regionAdmin);

            Role user = new Role("User");
            List<String> userPermission = new LinkedList<>();
            userPermission.add("corona_read");
            userPermission.add("user_read");
            user.addPermission(userPermission);
            roleService.initRole(user);




            repository.findAll().forEach(System.out::println);
		};
	}

	/**
	 * Populate the database with a few User entities
	 */
	@Bean
	CommandLineRunner userInit(UserRepository userRepository,UserService userService)
	{
		return args -> {
			Stream.of("all","americaUser","europeUser","asiaUser","africaUser","euroAsiaUser","americaAfricaUser").forEach(name -> {
				User user;
				ArrayList<String> realms=new ArrayList<>();
				ArrayList<Long> tenants=new ArrayList<>();
				if(name.equals("americaUser")) {
					tenants.add(2L);
					realms.add("America");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name, "u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else if(name.equals("europeUser")) {
					tenants.add(3L);
					realms.add("Europe");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else if(name.equals("asiaUser")) {
					tenants.add(4L);
					realms.add("Asia");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else if(name.equals("africaUser")){
					tenants.add(5L);
					realms.add("Africa");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else if(name.equals("all")){
					tenants.add(6L);
					realms.add("All");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else if(name.equals("euroAsiaUser")){
					tenants.add(3L);
					tenants.add(4L);
					realms.add("Europe");
					realms.add("Asia");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				else{
					tenants.add(2L);
					tenants.add(5L);
					realms.add("America");
					realms.add("Africa");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"u12", 17, tenants);
					userService.addKeycloakUser(user,realms,"User");
					user.encodePassword(user.getPassword());
				}
				userRepository.save(user);

			});
			userRepository.findAll().forEach(System.out::println);
		};
	}

	@Bean
	CommandLineRunner adminInit(UserRepository userRepository, UserService userService)
	{
		return args -> {
			Stream.of("Admin").forEach(name -> {
				ArrayList<String> realms=new ArrayList<>();
				ArrayList<Long> tenants=new ArrayList<>();
				tenants.add(1L);
				realms.add("Admin");
				User user = new User(name,name,name.toLowerCase() + "@domain.com","admin","admin",15,tenants);
				userService.addKeycloakUser(user,realms,"Admin");
				user.encodePassword(user.getPassword());
				userRepository.save(user);
			});
			userRepository.findAll().forEach(System.out::println);
		};
	}
	@Bean
	CommandLineRunner regionAdminInit(UserRepository userRepository, UserService userService)
	{
		return args -> {
			Stream.of("americaAdmin","europeAdmin","asiaAdmin","africaAdmin").forEach(name -> {
				User user;
				ArrayList<String> realms=new ArrayList<>();
				ArrayList<Long> tenants=new ArrayList<>();

				if(name.contains("america")) {
					tenants.add(2L);
					realms.add("America");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"a12", 16, tenants);
					userService.addKeycloakUser(user,realms,"Region-Admin");
					user.encodePassword(user.getPassword());
				}
				else if(name.contains("europe")) {
					tenants.add(3L);
					realms.add("Europe");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"a12", 16, tenants);
					userService.addKeycloakUser(user,realms,"Region-Admin");
					user.encodePassword(user.getPassword());
				}
				else if(name.contains("asia")) {
					tenants.add(4L);
					realms.add("Asia");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"a12", 16, tenants);
					userService.addKeycloakUser(user,realms,"Region-Admin");
					user.encodePassword(user.getPassword());
				}
				else {
					tenants.add(5L);
					realms.add("Africa");
					user = new User(name, name, name.toLowerCase() + "@domain.com", name,"a12", 16, tenants);
					userService.addKeycloakUser(user,realms,"Region-Admin");
					user.encodePassword(user.getPassword());
				}
				userRepository.save(user);
			});
			userRepository.findAll().forEach(System.out::println);
			Thread initThread=new Thread(){
				public void run(){
					userService.initKeycloakUsers(userRepository);
				}
			};
			initThread.start();
			settingsService.applySettings(initSettings);
		};
	}




}