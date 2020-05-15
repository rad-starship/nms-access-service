package com.rad.server.access.entities.settings;

public class PasswordPolicy {
    private int expirePassword;
    private int minimumLength;
    private int notRecentlyUsed;
    private int digits;
    private boolean notUsername;

    public PasswordPolicy(int expirePassword, int minimumLength, int notRecentlyUsed, int digits, boolean notUsername) {
        this.expirePassword = expirePassword;
        this.minimumLength = minimumLength;
        this.notRecentlyUsed = notRecentlyUsed;
        this.digits = digits;
        this.notUsername = notUsername;
    }

    public int getExpirePassword() {
        return expirePassword;
    }

    public void setExpirePassword(int expirePassword) {
        this.expirePassword = expirePassword;
    }

    public int getMinimumLength() {
        return minimumLength;
    }

    public void setMinimumLength(int minimumLength) {
        this.minimumLength = minimumLength;
    }

    public int getNotRecentlyUsed() {
        return notRecentlyUsed;
    }

    public void setNotRecentlyUsed(int notRecentlyUsed) {
        this.notRecentlyUsed = notRecentlyUsed;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public boolean isNotUsername() {
        return notUsername;
    }

    public void setNotUsername(boolean notUsername) {
        this.notUsername = notUsername;
    }
}
