package com.legal.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // API publica de la landing: solo los dominios permitidos. Va ANTES de /api/**
        // porque se usa la primera configuracion cuyo patron coincide.
        registry.addMapping("/api/v1/**")
                .allowedOrigins(this.allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");

        registry.addMapping("/api/**")
                .allowedOrigins("*") // En producción cámbialo por la URL de tu React
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
