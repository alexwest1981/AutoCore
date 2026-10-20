package com.wac.autocore.config;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.LoginService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Who is signed in, and how one gets there. The classes themselves know nothing of Spring; they
 * are only built here.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public IdentityHolder identityHolder() {
        return new IdentityHolder();
    }

    @Bean
    public UserRepository userRepository(Db db) {
        return new UserRepository(db);
    }

    @Bean
    public LoginService loginService(UserRepository users) {
        return new LoginService(users);
    }
}