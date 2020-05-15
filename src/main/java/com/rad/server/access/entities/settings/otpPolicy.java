package com.rad.server.access.entities.settings;

public class otpPolicy {
    public boolean enabled;
    private String optType;
    private int numberOfDigits;
    private int optTokenPeriod;

    public otpPolicy(boolean enabled, String optType, int numberOfDigits, int optTokenPeriod) {
        this.enabled = enabled;
        this.optType = optType;
        this.numberOfDigits = numberOfDigits;
        this.optTokenPeriod = optTokenPeriod;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOptType() {
        return optType;
    }

    public void setOptType(String optType) {
        this.optType = optType;
    }

    public int getNumberOfDigits() {
        return numberOfDigits;
    }

    public void setNumberOfDigits(int numberOfDigits) {
        this.numberOfDigits = numberOfDigits;
    }

    public int getOptTokenPeriod() {
        return optTokenPeriod;
    }

    public void setOptTokenPeriod(int optTokenPeriod) {
        this.optTokenPeriod = optTokenPeriod;
    }
}
