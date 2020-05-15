package com.rad.server.access.services;

import com.rad.server.access.entities.settings.Settings;
import org.apache.tomcat.util.json.ParseException;

public interface SettingsService {

     Settings parseSettings(Object settings) ;

    void applySettings(Settings settings1);
}
