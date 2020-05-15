package com.rad.server.access.entities.settings;

public class SocialLogin {
    private String identityProvider;

    public SocialLogin(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }
}
