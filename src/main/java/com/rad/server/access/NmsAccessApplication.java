package com.rad.server.access;

import java.net.*;
import java.util.stream.*;
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
	CommandLineRunner userInit(UserRepository userRepository)
	{
		return args -> {
			Stream.of("John", "Julie", "Jennifer", "Helen", "Rachel").forEach(name -> {
				User user = new User(name,name,name.toLowerCase() + "@domain.com",name);
				userRepository.save(user);
			});
			userRepository.findAll().forEach(System.out::println);
		};
	}
	
	/**
	 * Populate the database with a few User entities
	 */
	@Bean
	CommandLineRunner tenantInit(TenantRepository repository)
	{
		return args -> {
			Stream.of("Admin", "America", "Europe", "Asia", "Africa").forEach(name -> {
				Tenant t = new Tenant(name);
				repository.save(t);
			});
			repository.findAll().forEach(System.out::println);
		};
	}	
	
	/**
	 * Populate the database with a few User entities
	 */
	@Bean
	CommandLineRunner roleInit(RoleRepository repository)
	{
		return args -> {
			Stream.of("Admin", "ReginAdmin", "User").forEach(name -> {
				Role t = new Role(name);
				repository.save(t);
			});
			repository.findAll().forEach(System.out::println);
		};
	}	
}