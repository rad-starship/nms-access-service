package com.rad.server.access.entities;

public class Event {
    private String clientId;
    private String time;
    private String tenant;
    private String type;
    private String error;
    private String ip;
    private String details;

    public Event(String clientId,String time, String tenant, String type, String error, String ip, String details) {
        this.clientId = clientId;
        this.time = time;
        this.tenant = tenant;
        this.type = type;
        this.error = error;
        this.ip = ip;
        this.details = details;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String  time) {
        this.time = time;
    }
}
