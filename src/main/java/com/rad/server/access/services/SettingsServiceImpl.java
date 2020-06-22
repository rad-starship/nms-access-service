package com.rad.server.access.services;

import com.rad.server.access.componenets.KeycloakAdminProperties;
import com.rad.server.access.entities.Tenant;
import com.rad.server.access.entities.settings.*;
import com.rad.server.access.presistance.EsConnectionHandler;
import com.rad.server.access.repositories.TenantRepository;
import com.rad.server.access.responses.HttpResponse;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsServiceImpl implements SettingsService {

    private final boolean isOnline = true;

    @Autowired
    private KeycloakAdminProperties prop;
    
    @Autowired
    private TenantRepository repository;

//    @Autowired
//    Settings settings;



    
    private Keycloak getKeycloakInstance(){
        return Keycloak.getInstance(

                prop.getServerUrl(),// keycloak address
                prop.getRelm(), // ​​specify Realm master
                prop.getUsername(), // ​​administrator account
                prop.getPassword(), // ​​administrator password
                prop.getCliendId());
    }

    public Settings getSettings(){
        return null;
    }

    /**
     * The function goes over the Json received in the request and parse it to Settings Object.
     * @param settings - Json from request body, some fields may be missing.
     * @return Settings object with null on missing fields.
     */
    @Override
    public Settings parseSettings(Object settings) throws Exception {

            Authentication authentication;
            Autorization autorization;
            boolean events, isOnline;
            Map<String, Object> map = (Map<String, Object>) settings;

            Map<String, Object> authenticationMap = (Map<String, Object>) map.get("authentication");
            if (authenticationMap != null) {
                Token token;
                PasswordPolicy passwordPolicy;
                otpPolicy otpPolicy;
                SocialLogin socialLogin;

                Map<String, Object> tokenMap = (Map<String, Object>) authenticationMap.get("token");
                if (tokenMap != null) {
                    token = new Token((int) tokenMap.get("ssoSessionIdle"), (int) tokenMap.get("ssoSessionMax"), (int) tokenMap.get("offlineSessionIdle"), (int) tokenMap.get("accessTokenLifespan"));
                    if(token.getSsoSessionIdle()<=0 ||token.getSsoSessionMax()<=0||token.getOfflineSessionIdle()<=0||token.getAccessTokenLifespan()<=0)
                        throw new Exception();
                } else {
                    token = null;
                }

                Map<String, Object> passwordMap = (Map<String, Object>) authenticationMap.get("passwordPolicy");
                if (passwordMap != null) {
                    passwordPolicy = new PasswordPolicy((int) passwordMap.get("expirePassword"),
                            (int) passwordMap.get("minimumLength"),
                            (int) passwordMap.get("notRecentlyUsed"),
                            (int) passwordMap.get("digits"),
                            (boolean) passwordMap.get("notUsername"));
                    if(passwordPolicy.getMinimumLength()<=0 ||passwordPolicy.getNotRecentlyUsed()<=0||passwordPolicy.getDigits()<=0)
                        throw new Exception();
                } else {
                    passwordPolicy = null;
                }

                Map<String, Object> otpMap = (Map<String, Object>) authenticationMap.get("otpPolicy");
                if (otpMap != null) {
                    otpPolicy = new otpPolicy((boolean) otpMap.get("enabled"),
                            (String) otpMap.get("optType"),
                            (int) otpMap.get("numberOfDigits"),
                            (int) otpMap.get("optTokenPeriod")
                    );
                    if(otpPolicy.getOptTokenPeriod()<=0 ||otpPolicy.getNumberOfDigits()<=0)
                        throw new Exception();
                } else {
                    otpPolicy = null;
                }

                Map<String, Object> socialLoginMap = (Map<String, Object>) authenticationMap.get("socialLogin");
                if (socialLoginMap != null) {
                    socialLogin = new SocialLogin((String) socialLoginMap.get("identityProvider"));
                } else {
                    socialLogin = null;
                }

                authentication = new Authentication(token, passwordPolicy, otpPolicy, socialLogin);

            } else {
                authentication = null;
            }
            Map<String, Object> authorizationMap = (Map<String, Object>) map.get("authorization");
            if (authorizationMap != null) {
                autorization = new Autorization();
            } else {
                autorization = null;
            }
            if (map.get("events") != null)
                events = Boolean.valueOf((String) map.get("events"));
            else
                events = true;

            if (map.get("isOnline") != null)
                isOnline = Boolean.valueOf((String) map.get("isOnline"));
            else
                isOnline = true;

            return new Settings(authentication, autorization, events, isOnline);



    }

    /**
     * Runs over the Settings object and for each non null field apply relevant info in keycloak
     * NOTE: apply settings on all keycloak realms.
     * @param settings1 - Settings to be applied.
     */
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

        applyEvents(settings1.isEvents());
    }

    private void applyEvents(boolean events){
        Keycloak keycloak=getKeycloakInstance();
        List<String> eventTypes=new ArrayList<>();
        eventTypes.add("LOGIN");
        eventTypes.add("LOGIN_ERROR");
        eventTypes.add("LOGOUT");
        eventTypes.add("LOGOUT_ERROR");
        for(Tenant tenant: repository.findAll()) {
            RealmRepresentation realmRepresentation=keycloak.realm(tenant.getName()).toRepresentation();
            realmRepresentation.setEventsEnabled(events);
            realmRepresentation.setEnabledEventTypes(eventTypes);
            realmRepresentation.setEventsExpiration(24L);

            keycloak.realm(tenant.getName()).update(realmRepresentation);
        }

    }

    private void applyOtp(otpPolicy otpPolicy) {
        Keycloak keycloak = getKeycloakInstance();

        for(Tenant tenant: repository.findAll()){

            RealmRepresentation realmRepresentation =keycloak.realm(tenant.getName()).toRepresentation();
            realmRepresentation.setOtpPolicyDigits(otpPolicy.getNumberOfDigits());
            realmRepresentation.setOtpPolicyLookAheadWindow(otpPolicy.getOptTokenPeriod());
            if(otpPolicy.getOptType().equals("Time Based"))
            realmRepresentation.setOtpPolicyType("totp");
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

    @Override
    public Settings getFromEs() {
        Settings result = null;
        if(isOnline) {
            try {
                EsConnectionHandler.makeConnection();
                Map<String, Object> data = EsConnectionHandler.loadSettings();
                if (data != null)
                    result = parseSettings(data);

                EsConnectionHandler.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public void saveToEs(Settings tmpSettings) {
        if(isOnline) {
            try {
                EsConnectionHandler.makeConnection();
                EsConnectionHandler.saveSettings(tmpSettings);
                EsConnectionHandler.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateES(Settings settings1) {
        if (isOnline) {
            try {
                EsConnectionHandler.makeConnection();
                EsConnectionHandler.deleteSettings();
                EsConnectionHandler.saveSettings(settings1);
                EsConnectionHandler.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
