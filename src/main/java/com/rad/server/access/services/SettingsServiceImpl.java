package com.rad.server.access.services;

import com.rad.server.access.entities.settings.*;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingsServiceImpl implements SettingsService {
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
                token = new Token((int)tokenMap.get("ssoSessionIdle"),(int)tokenMap.get("ssoSessionMax"));
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
        Authentication authentication = settings1.getAuthentication();
        if(authentication!=null){
            if(authentication.getSocialLogin()!= null){
                String provider = authentication.getSocialLogin().getIdentityProvider();
                if(provider.equals("google")){
                    setGoogleProvider();
                }
                if(provider.equals("facebook")){
                    setFacebookProvider();
                }
            }
        }
    }

    private void setFacebookProvider() {
    }

    private void setGoogleProvider() {
    }
}
