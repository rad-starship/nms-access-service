package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.settings.*;
import com.rad.server.access.repositories.TenantRepository;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingsServiceImpl implements SettingsService {

    @Autowired
    private KeycloakAdminProperties prop;
    
    @Autowired
    private TenantRepository repository;

    
    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }
    
    @Override
    public Settings parseSettings(Object settings)  {

        Authentication authentication;
        Autorization autorization;
        Map<String,Object> map = (LinkedHashMap<String, Object>)settings;

        Map<String,Object> authenticationMap = (Map<String, Object>) map.get("authentication");
        if(authenticationMap!=null){
            Token token;
            PasswordPolicy passwordPolicy;
            otpPolicy otpPolicy;
            SocialLogin socialLogin;

            Map<String,Object> tokenMap = (Map<String, Object>) authenticationMap.get("token");
            if(tokenMap!=null){
                token = new Token((int)tokenMap.get("ssoSessionIdle"),(int)tokenMap.get("ssoSessionMax"),(int)tokenMap.get("offlineSessionIdle"),(int)tokenMap.get("accessTokenLifespan"));
            }
            else{
                token = null;
            }

            Map<String,Object> passwordMap = (Map<String, Object>) authenticationMap.get("passwordPolicy");
            if(passwordMap!=null){
                passwordPolicy = new PasswordPolicy((int)passwordMap.get("expirePassword"),
                                                    (int)passwordMap.get("minimumLength"),
                                                    (int)passwordMap.get("notRecentlyUsed"),
                                                    (int)passwordMap.get("digits"),
                                                    (boolean)passwordMap.get("notUsername"));
            }
            else{
                passwordPolicy = null;
            }

            Map<String,Object> otpMap = (Map<String, Object>) authenticationMap.get("otpPolicy");
            if(otpMap!=null){
                otpPolicy = new otpPolicy((boolean)otpMap.get("enabled"),
                                        (String)otpMap.get("optType"),
                                        (int)otpMap.get("numberOfDigits"),
                                        (int)otpMap.get("optTokenPeriod")
                                         );
            }
            else{
                otpPolicy = null;
            }

            Map<String,Object> socialLoginMap = (Map<String, Object>) authenticationMap.get("socialLogin");
            if(socialLoginMap!=null){
                socialLogin = new SocialLogin((String)socialLoginMap.get("identityProvider"));
            }
            else{
                socialLogin = null;
            }

            authentication = new Authentication(token,passwordPolicy,otpPolicy,socialLogin);

        }
        else{
            authentication = null;
        }
        Map<String,Object> authorizationMap = (Map<String, Object>) map.get("authorization");
        if(authorizationMap!=null){
            autorization = new Autorization();
        }
        else{
            autorization = null;
        }
        return new Settings(authentication,autorization);

    }

    @Override
    public void applySettings(Settings settings1) {
        //For now updates all realms. maybe change to specific one later..
        Authentication authentication = settings1.getAuthentication();
        if(authentication!=null){
            if(authentication.getSocialLogin()!= null){
                applySocialProvider(authentication.getSocialLogin());
            }
            if(authentication.getPasswordPolicy() != null){
                applyPasswordPolicy(authentication.getPasswordPolicy());
            }
            if(authentication.getToken()!=null){
                applyToken(authentication.getToken());
            }
            if(authentication.getOtpPolicy()!=null){
                applyOtp(authentication.getOtpPolicy());
            }
        }
    }

    private void applyOtp(otpPolicy otpPolicy) {
        Keycloak keycloak = getKeycloakInstance();

        for(Tenant tenant: repository.findAll()){

            RealmRepresentation realmRepresentation =keycloak.realm(tenant.getName()).toRepresentation();
            realmRepresentation.setOtpPolicyDigits(otpPolicy.getNumberOfDigits());
            realmRepresentation.setOtpPolicyLookAheadWindow(otpPolicy.getOptTokenPeriod());
            realmRepresentation.setOtpPolicyType(otpPolicy.getOptType());
            keycloak.realm(tenant.getName()).update(realmRepresentation);
        }
    }

    private void applyToken(Token token) {
        Keycloak keycloak = getKeycloakInstance();

        for(Tenant tenant: repository.findAll()){

            RealmRepresentation realmRepresentation =keycloak.realm(tenant.getName()).toRepresentation();
            applyTokenToRealm(token,realmRepresentation);
            keycloak.realm(tenant.getName()).update(realmRepresentation);
        }
    }

    private void applyPasswordPolicy(PasswordPolicy passwordPolicy) {
        String password="length("+passwordPolicy.getMinimumLength()+") and forceExpiredPasswordChange("+passwordPolicy.getExpirePassword()+") and digits("+passwordPolicy.getDigits()+") and passwordHistory("+passwordPolicy.getNotRecentlyUsed()+")";
        if(passwordPolicy.isNotUsername())
            password+=" and notUsername(undefined)";
        Keycloak keycloak = getKeycloakInstance();

        for(Tenant tenant: repository.findAll()){
            
            RealmRepresentation realmRepresentation =keycloak.realm(tenant.getName()).toRepresentation();
            realmRepresentation.setPasswordPolicy(password);
            keycloak.realm(tenant.getName()).update(realmRepresentation);
        }
        
    }

    private void applySocialProvider(SocialLogin sLogin) {
        String provider = sLogin.getIdentityProvider();
        if(provider.equals("google")){
            setGoogleProvider();
        }
        if(provider.equals("facebook")){
            setFacebookProvider();
        }
    }

    private void setFacebookProvider() {
    }

    private void setGoogleProvider() {
    }

    public void applyTokenToRealm(Token token, RealmRepresentation realm) {
        realm.setSsoSessionIdleTimeout(token.getSsoSessionIdle()*60);
        realm.setSsoSessionMaxLifespan(token.getSsoSessionMax()*60);
        realm.setOfflineSessionIdleTimeout(token.getOfflineSessionIdle()*60);
        realm.setAccessTokenLifespan(token.getAccessTokenLifespan()*60);
    }
}
