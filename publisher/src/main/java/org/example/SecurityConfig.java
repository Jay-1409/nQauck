package org.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.example.Security.DashboardAccountStore;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/", "/setup", "/setup.html", "/csrf").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .formLogin(form -> form.defaultSuccessUrl("/index.html", true))
                .build();
    }

    @Bean
    InMemoryUserDetailsManager userDetailsService(DashboardAccountStore accountStore) {
        var users = new InMemoryUserDetailsManager();
        accountStore.load().ifPresent(account -> users.createUser(User.withUsername(account.username())
                .password(account.passwordHash())
                .roles("USER")
                .build()));
        return users;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
