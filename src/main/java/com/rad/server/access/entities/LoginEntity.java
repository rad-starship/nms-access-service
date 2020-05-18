package com.rad.server.access.entities;

public class LoginEntity {
    String username;
    String password;
    String otp;
    String tenant;

    public LoginEntity(String username, String password, String otp, String tenant) {
        this.username = username;
        this.password = password;
        this.otp = otp;
        this.tenant = tenant;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}
