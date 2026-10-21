package com.wac.autocore.config;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.service.PasswordRotation;

import org.springframework.beans.factory.annotation.Value;
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

    /**
     * The demo accounts' passwords as the configuration gives them. An empty value means the
     * migration's account is kept, so a run without any variable behaves exactly as before.
     */
    @Bean
    public PasswordRotation passwordRotation(UserRepository users,
            @Value("${autocore.password.admin:}") String admin,
            @Value("${autocore.password.staff:}") String staff,
            @Value("${autocore.password.readonly:}") String readOnly) {
        java.util.Map<String, String> configured = new java.util.LinkedHashMap<String, String>();
        configured.put("admin", admin);
        configured.put("staff", staff);
        configured.put("readonly", readOnly);
        return new PasswordRotation(users, configured);
    }
}