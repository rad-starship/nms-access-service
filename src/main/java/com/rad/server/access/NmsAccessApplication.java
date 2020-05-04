package com.rad.server.access;

import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.*;

import com.rad.server.access.services.TenantService;

import com.rad.server.access.services.RoleService;
import com.rad.server.access.services.UserService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import com.rad.server.access.entities.*;
import com.rad.server.access.entities.Role;
import com.rad.server.access.repositories.*;

@SpringBootApplication
public class NmsAccessApplication implements ApplicationListener<ApplicationReadyEvent>
{
    @Autowired
    private ApplicationContext applicationContext;
	
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
	

	
	/**
	 * Populate the database with a few User entities
	 */
	@Bean
	CommandLineRunner tenantInit(TenantRepository repository, TenantService tenantService)
	{
		return args -> {
			Stream.of("Admin", "America", "Europe", "Asia", "Africa","All").forEach(name -> {
				Tenant t = new Tenant(name);
				repository.save(t);
			});
			repository.findAll().forEach(System.out::println);
			tenantService.initKeycloakTenants(repository);
		};
	}	
	
	/**
	 * Populate the database with a few User entities
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
			Stream.of("John", "Julie", "Jennifer", "Helen", "Rachel").forEach(name -> {
				User user = new User(name,name,name.toLowerCase() + "@domain.com",name);
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
				User user = new User(name,name,name.toLowerCase() + "@domain.com","admin",20,6);
				userRepository.save(user);
				userService.addKeycloakUser(user,"Admin","Admin");
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
				if(name.contains("america")) {
					user = new User(name, name, name.toLowerCase() + "@domain.com", name, 21, 7);
					userService.addKeycloakUser(user,"America","Region-Admin");
				}
				else if(name.contains("europe")) {
					user = new User(name, name, name.toLowerCase() + "@domain.com", name, 21, 8);
					userService.addKeycloakUser(user,"Europe","Region-Admin");
				}
				else if(name.contains("asia")) {
					user = new User(name, name, name.toLowerCase() + "@domain.com", name, 21, 9);
					userService.addKeycloakUser(user,"Asia","Region-Admin");
				}
				else {
					user = new User(name, name, name.toLowerCase() + "@domain.com", name, 21, 10);
					userService.addKeycloakUser(user,"Africa","Region-Admin");
				}
				userRepository.save(user);

			});
			userRepository.findAll().forEach(System.out::println);
		};
	}
}