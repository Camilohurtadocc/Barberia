package co.com.barberia.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el binding de {@link GatewaySecurityProperties} para que el filtro JWT
 * reciba el secreto y la lista de rutas públicas desde {@code application.yaml}.
 */
@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SecurityConfig {
}
