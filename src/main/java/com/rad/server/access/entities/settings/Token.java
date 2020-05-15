package com.rad.server.access.entities.settings;

public class Token {
    private int ssoSessionIdle;
    private int ssoSessionMax;

    public Token(int ssoSessionIdle, int ssoSessionMax) {
        this.ssoSessionIdle = ssoSessionIdle;
        this.ssoSessionMax = ssoSessionMax;
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
}
