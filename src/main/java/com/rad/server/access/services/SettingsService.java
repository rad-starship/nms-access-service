package com.rad.server.access.services;

import com.rad.server.access.entities.settings.Settings;
import com.rad.server.access.entities.settings.Token;
import org.apache.tomcat.util.json.ParseException;
import org.keycloak.representations.idm.RealmRepresentation;

public interface SettingsService {

     Settings parseSettings(Object settings) ;

    void applySettings(Settings settings1);
    void applyTokenToRealm(Token token, RealmRepresentation realm);

    Settings getFromEs();

    void saveToEs(Settings tmpSettings);

    void updateES(Settings settings1);
}
