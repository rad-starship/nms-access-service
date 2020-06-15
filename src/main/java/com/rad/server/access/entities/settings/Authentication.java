package com.rad.server.access.entities.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Authentication {

    private Token token;
    private PasswordPolicy passwordPolicy;
    private otpPolicy otpPolicy;
    private SocialLogin socialLogin;

    public Authentication(Token token, PasswordPolicy passwordPolicy, com.rad.server.access.entities.settings.otpPolicy otpPolicy, SocialLogin socialLogin) {
        this.token = token;
        this.passwordPolicy = passwordPolicy;
        this.otpPolicy = otpPolicy;
        this.socialLogin = socialLogin;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public com.rad.server.access.entities.settings.otpPolicy getOtpPolicy() {
        return otpPolicy;
    }

    public void setOtpPolicy(com.rad.server.access.entities.settings.otpPolicy otpPolicy) {
        this.otpPolicy = otpPolicy;
    }

    public SocialLogin getSocialLogin() {
        return socialLogin;
    }

    public void setSocialLogin(SocialLogin socialLogin) {
        this.socialLogin = socialLogin;
    }

    public String getJson(){
        boolean first = true;
        ObjectMapper mapper = new ObjectMapper();
        String output = "{";
        try {
            if (token != null) {
                if (first) {
                    first = false;
                } else {
                    output += ",";
                }

                output += "\"token\":" + mapper.writeValueAsString(token);


            }
            if (passwordPolicy != null) {
                if (first) {
                    first = false;
                } else {
                    output += ",";
                }
                output += "\"passwordPolicy\":"+mapper.writeValueAsString(passwordPolicy);

            }
            if (otpPolicy != null) {
                if (first) {
                    first = false;
                } else {
                    output += ",";
                }
                output += "\"otpPolicy\":"+mapper.writeValueAsString(otpPolicy);

            }
            if (socialLogin != null) {
                if (first) {
                    first = false;
                } else {
                    output += ",";
                }
                output += "\"socialLogin\":"+mapper.writeValueAsString(socialLogin);
            }
        } catch (JsonProcessingException e) {
        e.printStackTrace();
    }
        output+="}";
        return output;
    }
}
