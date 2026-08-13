package co.com.barberia.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS global para todo el gateway.
 *
 * <p>Se implementa como {@link CorsWebFilter} (un {@code WebFilter} del stack reactivo) y no
 * con {@code spring.cloud.gateway.server.webflux.globalcors}, por dos razones:
 * <ul>
 *     <li>El {@code WebFilter} corre antes que el enrutamiento, así que las respuestas de
 *         error del filtro JWT (401) también salen con cabeceras CORS y el navegador puede
 *         leer el status en lugar de reportar un error opaco de CORS.</li>
 *     <li>Evita configurar CORS por dos vías a la vez, que duplicaría la cabecera
 *         {@code Access-Control-Allow-Origin} y el navegador rechazaría la respuesta.</li>
 * </ul>
 *
 * <p>Se usa {@code allowCredentials=false} porque el token viaja en la cabecera
 * {@code Authorization} (leído de localStorage), no en cookies. Esa es la única
 * combinación válida junto a un origen comodín {@code *}.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(CorsConfiguration.ALL));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }
}
