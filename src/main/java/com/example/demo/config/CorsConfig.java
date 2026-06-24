package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS centralizada.
 *
 * El origen permitido NO se hardcodea: se inyecta desde una variable de
 * entorno (APP_CORS_ALLOWED_ORIGINS) que en el task definition de ECS
 * apuntará al dominio real de CloudFront en producción, y que en local
 * apunta a los puertos típicos de Angular (4200) y Vite/Vue (5173).
 *
 * Esto evita tener que reconstruir la imagen Docker cuando cambie el
 * dominio del frontend: solo hay que actualizar la variable de entorno
 * en el task definition (vía Terraform/CDK) y redesplegar el servicio.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Acepta una lista separada por comas, ej:
                // "https://app.midominio.com,https://d111111abcdef8.cloudfront.net"
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // Sin esto, un frontend que mande cookies/credenciales con
                // fetch(..., { credentials: "include" }) recibirá un error CORS
                .allowCredentials(true)
                .maxAge(3600);
    }
}
