package com.rad.server.access.adapters;

import com.rad.server.access.Configuration.MultitenantConfigResolver;
import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.keycloak.adapters.springboot.KeycloakAutoConfiguration;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolverWrapper;
import org.keycloak.adapters.springboot.KeycloakSpringBootProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(KeycloakSpringBootProperties.class)
public class MultitenantConfiguration extends KeycloakAutoConfiguration {
    //REFERENCE: https://github.com/vimalKeshu/movie-app
    private KeycloakSpringBootProperties m_keycloakProperties;

    @Autowired
    @Override
    public void setKeycloakSpringBootProperties(final KeycloakSpringBootProperties keycloakProperties) {
        m_keycloakProperties = keycloakProperties;
        super.setKeycloakSpringBootProperties(keycloakProperties);
        MultitenantConfigResolver.setAdapterConfig(keycloakProperties);
    }

    @Bean
    @ConditionalOnClass(name = {"org.apache.catalina.startup.Tomcat"})
    @Override
    public TomcatContextCustomizer tomcatKeycloakContextCustomizer() {
        return new MultitenantTomcatContextCustomizer(m_keycloakProperties);
    }

    static class MultitenantTomcatContextCustomizer extends KeycloakTomcatContextCustomizer {
        public MultitenantTomcatContextCustomizer(final KeycloakSpringBootProperties keycloakProperties)
        {
            super(keycloakProperties);
        }

        @Override
        public void customize(final Context context) {
            super.customize(context);
            final String name = "keycloak.config.resolver";
            context.removeParameter(name);
            context.addParameter(name, MultitenantConfigResolver.class.getName());
        }
    }

    static class KeycloakTomcatContextCustomizer extends KeycloakBaseTomcatContextCustomizer implements TomcatContextCustomizer {
        public KeycloakTomcatContextCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            super(keycloakProperties);
        }
    }

    static class KeycloakBaseTomcatContextCustomizer {
        protected final KeycloakSpringBootProperties keycloakProperties;

        public KeycloakBaseTomcatContextCustomizer(KeycloakSpringBootProperties keycloakProperties) {
            this.keycloakProperties = keycloakProperties;
        }

        public void customize(Context context) {
            LoginConfig loginConfig = new LoginConfig();
            loginConfig.setAuthMethod("KEYCLOAK");
            context.setLoginConfig(loginConfig);
            Set<String> authRoles = new HashSet();
            Iterator var4 = this.keycloakProperties.getSecurityConstraints().iterator();

            KeycloakSpringBootProperties.SecurityConstraint constraint;
            while(var4.hasNext()) {
                constraint = (KeycloakSpringBootProperties.SecurityConstraint)var4.next();
                Iterator var6 = constraint.getAuthRoles().iterator();

                while(var6.hasNext()) {
                    String authRole = (String)var6.next();
                    if (!authRoles.contains(authRole)) {
                        context.addSecurityRole(authRole);
                        authRoles.add(authRole);
                    }
                }
            }

            var4 = this.keycloakProperties.getSecurityConstraints().iterator();

            label81:
            while(var4.hasNext()) {
                constraint = (KeycloakSpringBootProperties.SecurityConstraint)var4.next();
                org.apache.tomcat.util.descriptor.web.SecurityConstraint tomcatConstraint = new org.apache.tomcat.util.descriptor.web.SecurityConstraint();
                Iterator var13 = constraint.getAuthRoles().iterator();

                while(true) {
                    String authRole;
                    do {
                        if (!var13.hasNext()) {
                            var13 = constraint.getSecurityCollections().iterator();

                            while(var13.hasNext()) {
                                KeycloakSpringBootProperties.SecurityCollection collection = (KeycloakSpringBootProperties.SecurityCollection)var13.next();
                                org.apache.tomcat.util.descriptor.web.SecurityCollection tomcatSecCollection = new org.apache.tomcat.util.descriptor.web.SecurityCollection();
                                if (collection.getName() != null) {
                                    tomcatSecCollection.setName(collection.getName());
                                }

                                if (collection.getDescription() != null) {
                                    tomcatSecCollection.setDescription(collection.getDescription());
                                }

                                Iterator var10 = collection.getPatterns().iterator();

                                String method;
                                while(var10.hasNext()) {
                                    method = (String)var10.next();
                                    tomcatSecCollection.addPattern(method);
                                }

                                var10 = collection.getMethods().iterator();

                                while(var10.hasNext()) {
                                    method = (String)var10.next();
                                    tomcatSecCollection.addMethod(method);
                                }

                                var10 = collection.getOmittedMethods().iterator();

                                while(var10.hasNext()) {
                                    method = (String)var10.next();
                                    tomcatSecCollection.addOmittedMethod(method);
                                }

                                tomcatConstraint.addCollection(tomcatSecCollection);
                            }

                            context.addConstraint(tomcatConstraint);
                            continue label81;
                        }

                        authRole = (String)var13.next();
                        tomcatConstraint.addAuthRole(authRole);
                    } while(!authRole.equals("*") && !authRole.equals("**"));

                    tomcatConstraint.setAuthConstraint(true);
                }
            }

            context.addParameter("keycloak.config.resolver", KeycloakSpringBootConfigResolverWrapper.class.getName());
        }
    }
}