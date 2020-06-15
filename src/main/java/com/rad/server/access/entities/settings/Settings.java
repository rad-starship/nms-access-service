package com.rad.server.access.entities.settings;

public class Settings {
    Authentication authentication;
    Autorization authorization;
    boolean events;

    public Settings(Authentication authentication, Autorization authorization,boolean events) {
        this.authentication = authentication;
        this.authorization = authorization;
        this.events=events;
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
