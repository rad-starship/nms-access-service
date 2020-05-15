package com.rad.server.access.entities.settings;

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
}
