package com.vanokal0.warehouse_backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;
import java.util.TimeZone;

/**
 * The PostgreSQL JDBC driver sends the JVM default time zone to the server on connect,
 * and PostgreSQL rejects legacy aliases (e.g. Europe/Kiev) that are missing from its tzdata.
 * Runs before any bean (including the DataSource) is created, both in the app and in tests.
 */
public class LegacyTimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Map<String, String> LEGACY_TIME_ZONES = Map.of(
            "Europe/Kiev", "Europe/Kyiv",
            "Europe/Uzhgorod", "Europe/Kyiv",
            "Europe/Zaporozhye", "Europe/Kyiv"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String replacement = LEGACY_TIME_ZONES.get(TimeZone.getDefault().getID());
        if (replacement != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(replacement));
        }
    }
}
