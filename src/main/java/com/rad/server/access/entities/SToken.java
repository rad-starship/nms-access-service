package com.rad.server.access.entities;


public class SToken {
    private String token;

    public SToken(String token) {
        this.token = token;
    }

    public SToken(Object token) {
        this.token = String.valueOf(token);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
