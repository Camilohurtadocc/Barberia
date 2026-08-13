package co.com.barberia.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración de seguridad del gateway (prefijo {@code gateway.security}).
 *
 * @param secret          clave HMAC compartida con servicio-auth. Debe ser idéntica a la
 *                        usada al firmar el token, o toda validación fallará con 401.
 * @param publicEndpoints rutas que no exigen token JWT.
 * @param roleRules       rutas que además exigen un rol concreto.
 */
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(String secret,
                                        List<PublicEndpoint> publicEndpoints,
                                        List<RoleRule> roleRules) {

    public GatewaySecurityProperties {
        publicEndpoints = publicEndpoints == null ? List.of() : List.copyOf(publicEndpoints);
        roleRules = roleRules == null ? List.of() : List.copyOf(roleRules);
    }

    /**
     * @param method método HTTP permitido sin token; {@code *} o vacío significa "cualquiera".
     * @param pattern patrón de ruta estilo Spring (por ejemplo {@code /api/**}).
     */
    public record PublicEndpoint(String method, String pattern) {
    }

    /**
     * Restringe una ruta a ciertos roles.
     *
     * <p>Estas reglas son un primer filtro grueso: cortan lo que se puede decidir
     * mirando solo el método y la ruta (por ejemplo, "crear barberos es de ADMIN").
     * Las reglas de pertenencia —"este barbero solo edita SU ficha"— no se pueden
     * resolver aquí porque hace falta consultar el dato, y viven en cada
     * microservicio.
     *
     * @param method  método HTTP; {@code *} o vacío significa "cualquiera".
     * @param pattern patrón de ruta.
     * @param roles   roles autorizados. Quien no traiga uno de estos recibe 403.
     */
    public record RoleRule(String method, String pattern, List<String> roles) {

        public RoleRule {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
