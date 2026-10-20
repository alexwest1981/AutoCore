package com.wac.autocore.config;

import com.wac.autocore.security.IdentityHolder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The identity holder for the app. The class itself knows nothing of Spring; it is only built here. */
@Configuration
public class SecurityConfig {

    @Bean
    public IdentityHolder identityHolder() {
        return new IdentityHolder();
    }
}