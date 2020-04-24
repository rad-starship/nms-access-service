package com.rad.server.access.componenets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("kc-admin")
public class KeycloakAdminProperties {
    private String serverUrl;
    private String relm;
    private String username;
    private String password;

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setRelm(String relm) {
        this.relm = relm;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    private String cliendId;

    public String getServerUrl() {
        return serverUrl;
    }

    public String getRelm() {
        return relm;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCliendId() {
        return cliendId;
    }
}
