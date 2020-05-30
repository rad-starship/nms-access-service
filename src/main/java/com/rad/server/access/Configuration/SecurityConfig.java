package com.rad.server.access.Configuration;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter
{
    /**
     * Registers the KeycloakAuthenticationProvider with the authentication manager.
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        KeycloakAuthenticationProvider keyCloakAuthProvider = keycloakAuthenticationProvider();
        keyCloakAuthProvider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        auth.authenticationProvider(keyCloakAuthProvider);
    }

    /**
     * Defines the session authentication strategy.
     */
    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy()
    {
        return new NullAuthenticatedSessionStrategy();
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        super.configure(http);
        http
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy( SessionCreationPolicy.STATELESS )
                .sessionAuthenticationStrategy(sessionAuthenticationStrategy())
                .and()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET,"/users/*").permitAll()
                .antMatchers(HttpMethod.POST,"/users/*").hasAnyRole("Admin","Region-Admin","user_write","all")
                .antMatchers(HttpMethod.PUT,"/users/*").hasAnyRole("Admin","Region-Admin","user_write","all")
                .antMatchers(HttpMethod.DELETE,"/users/*").hasAnyRole("Admin","Region-Admin","user_write","all")
                .antMatchers(HttpMethod.GET,"/roles/*").hasAnyRole("Admin","Region-Admin","role_read","all")
                .antMatchers(HttpMethod.POST,"/roles/*").hasAnyRole("Admin","Region-Admin","role_write","all")
                .antMatchers(HttpMethod.PUT,"/roles/*").hasAnyRole("Admin","Region-Admin","role_write","all")
                .antMatchers(HttpMethod.DELETE,"/roles/*").hasAnyRole("Admin","Region-Admin","role_write","all")
                .antMatchers(HttpMethod.GET,"/tenants/*").hasAnyRole("Admin","Region-Admin","tenant_read","all")
                .antMatchers(HttpMethod.POST,"/tenants/*").hasAnyRole("Admin","all")
                .antMatchers(HttpMethod.PUT,"/tenants/*").hasAnyRole("Admin","all")
                .antMatchers(HttpMethod.DELETE,"/tenants/*").hasAnyRole("Admin","all")
                .antMatchers(HttpMethod.POST,"/settings/*").hasAnyRole("Admin","all")
                .antMatchers(HttpMethod.GET,"/corona/*").permitAll();

    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/login");
    }


    @Autowired
    public KeycloakClientRequestFactory keycloakClientRequestFactory;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public KeycloakClientRequestFactory KeycloakClientRequestFactory()
    {
        return new KeycloakClientRequestFactory();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public KeycloakRestTemplate keycloakRestTemplate() {
        return new KeycloakRestTemplate(keycloakClientRequestFactory);
    }

    @Bean
    @Scope(scopeName = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AccessToken getKeycloakSecurityContext() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder
                        .currentRequestAttributes()).getRequest();
        KeycloakAuthenticationToken principal = (KeycloakAuthenticationToken) request.getUserPrincipal();
        return  ((KeycloakAuthenticationToken) request.getUserPrincipal()).getAccount().getKeycloakSecurityContext().getToken();
    }



    /**
     * Sets keycloaks config resolver to use springs application.properties instead of keycloak.json (which is standard)
     * @return
     */
    @Bean
    public KeycloakConfigResolver KeycloakConfigResolver() {
        return new MultitenantConfigResolver();
    }
}