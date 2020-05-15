package com.rad.server.access.entities.settings;

public class Settings {
    Authentication authentication;
    Autorization authorization;

    public Settings(Authentication authentication, Autorization authorization) {
        this.authentication = authentication;
        this.authorization = authorization;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Autorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Autorization authorization) {
        this.authorization = authorization;
    }
}
