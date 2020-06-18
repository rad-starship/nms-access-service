package com.rad.server.access.entities.settings;

public class Settings {
    Authentication authentication;
    Autorization authorization;
    boolean events;
    boolean isOnline;

    public Settings(Authentication authentication, Autorization authorization,boolean events,boolean isOnline) {
        this.authentication = authentication;
        this.authorization = authorization;
        this.events=events;
        this.isOnline=isOnline;
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

    public boolean isEvents() {
        return events;
    }

    public void setEvents(boolean events) {
        this.events = events;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getJson(){
        boolean first = true;
        String output = "{";
        if(authentication!=null){
            if (first) {
                first = false;
            } else {
                output += ",";
            }
            output += "\"authentication\":"+authentication.getJson();
        }
        if(authorization!=null){
            if (first) {
                first = false;
            } else {
                output += ",";
            }
            output += "\"authorization\":"+authorization.getJson();
        }

        if (first) {
            first = false;
        } else {
            output += ",";
        }
        output+="\"events\":\""+isEvents()+"\"";

        output+="}";
        return output;
    }
}
