package com.legal.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (Webhook de Meta y Health)
                .requestMatchers("/webhook/**", "/health", "/error").permitAll()
                // Rutas privadas (El panel)
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults()); // Usamos Basic Auth

        return http.build();
    }

    // Aquí defines los usuarios y contraseñas (Cámbialos por los tuyos)
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}diligencias1488") // {noop} significa "sin encriptar" (ideal para pruebas rápidas)
                .roles("ADMIN")
                .build();
                
        UserDetails socio1 = User.withUsername("fernando")
                .password("{noop}fernando123")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin, socio1);
    }
}