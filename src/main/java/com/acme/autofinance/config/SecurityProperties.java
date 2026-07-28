package com.acme.autofinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final User user = new User();
    private final User admin = new User();

    /**
     * Set to false in every non-local environment; when true the application logs a warning that
     * the credentials are the well-known development defaults committed to this repository.
     */
    private boolean usingDevelopmentDefaults;

    public boolean isUsingDevelopmentDefaults() {
        return usingDevelopmentDefaults;
    }

    public void setUsingDevelopmentDefaults(boolean usingDevelopmentDefaults) {
        this.usingDevelopmentDefaults = usingDevelopmentDefaults;
    }

    public User getUser() {
        return user;
    }

    public User getAdmin() {
        return admin;
    }

    public static class User {

        /** Login name. */
        private String username;

        /** BCrypt hash of the password (never a plaintext value). */
        private String passwordHash;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }
    }
}
