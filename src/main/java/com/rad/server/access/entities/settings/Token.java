package com.rad.server.access.entities.settings;

public class Token {
    private int ssoSessionIdle;
    private int ssoSessionMax;
    private int offlineSessionIdle;
    private int accessTokenLifespan;

    public Token(int ssoSessionIdle, int ssoSessionMax, int offlineSessionIdle,int accessTokenLifespan) {
        this.ssoSessionIdle = ssoSessionIdle;
        this.ssoSessionMax = ssoSessionMax;
        this.offlineSessionIdle=offlineSessionIdle;
        this.accessTokenLifespan=accessTokenLifespan;
    }

    public int getSsoSessionIdle() {
        return ssoSessionIdle;
    }

    public void setSsoSessionIdle(int ssoSessionIdle) {
        this.ssoSessionIdle = ssoSessionIdle;
    }

    public int getSsoSessionMax() {
        return ssoSessionMax;
    }

    public void setSsoSessionMax(int ssoSessionMax) {
        this.ssoSessionMax = ssoSessionMax;
    }

    public int getOfflineSessionIdle() {
        return offlineSessionIdle;
    }

    public void setOfflineSessionIdle(int offlineSessionIdle) {
        this.offlineSessionIdle = offlineSessionIdle;
    }

    public int getAccessTokenLifespan() {
        return accessTokenLifespan;
    }

    public void setAccessTokenLifespan(int accessTokenLifespan) {
        this.accessTokenLifespan = accessTokenLifespan;
    }
}
