package co.com.barberia.gateway.filter;

import co.com.barberia.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Valida el token JWT de forma centralizada, antes de enrutar hacia los microservicios.
 *
 * <p>Los microservicios de negocio no verifican el token: confían en que todo tráfico entra
 * por aquí. Por eso este filtro también <em>borra</em> las cabeceras de identidad que vengan
 * del cliente antes de reescribirlas, de modo que nadie pueda suplantar a un usuario
 * mandando {@code X-Auth-User} o {@code X-Auth-Barbero} a mano.
 *
 * <p>Hace dos cosas en orden:
 * <ol>
 *   <li><b>Autenticación</b>: lo declarado en {@code public-endpoints} pasa sin token; el
 *       resto exige un {@code Authorization: Bearer <token>} HS256 válido y no expirado,
 *       o responde {@code 401}.</li>
 *   <li><b>Autorización</b>: además, lo declarado en {@code role-rules} exige un rol
 *       concreto, o responde {@code 403}.</li>
 * </ol>
 *
 * <p>La diferencia importa: 401 significa "no sé quién eres", 403 significa "sé quién eres
 * y no te toca". El frontend cierra sesión ante un 401, pero ante un 403 solo avisa.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_HEADER = "X-Auth-User";
    public static final String ROLE_HEADER = "X-Auth-Role";
    /** Id del barbero dueño de la sesión. Null en cuentas ADMIN. */
    public static final String BARBERO_HEADER = "X-Auth-Barbero";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ANY_METHOD = "*";

    private final SecretKey signingKey;
    private final List<EndpointMatcher> publicEndpoints;
    private final List<RoleMatcher> roleRules;

    public JwtAuthenticationFilter(GatewaySecurityProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        PathPatternParser parser = new PathPatternParser();
        this.publicEndpoints = properties.publicEndpoints().stream()
                .map(endpoint -> new EndpointMatcher(
                        resolveMethod(endpoint.method()),
                        parser.parse(endpoint.pattern())))
                .toList();
        this.roleRules = properties.roleRules().stream()
                .map(rule -> new RoleMatcher(
                        new EndpointMatcher(resolveMethod(rule.method()), parser.parse(rule.pattern())),
                        rule.roles()))
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublic(request)) {
            // Una ruta pública se atiende con o sin token, pero si el token viene y es
            // válido hay que propagar la identidad igualmente.
            //
            // Antes aquí se mandaba siempre (null, null, null) y eso rompía la reserva
            // de un cliente con la sesión abierta: POST /api/citas es público (reservar
            // sin cuenta sigue permitido), así que la petición llegaba anónima y la cita
            // se guardaba sin dueño. El cliente reservaba y su historial seguía vacío.
            //
            // La identidad sigue saliendo SOLO de una firma verificada: un token ausente
            // o manipulado deja la petición anónima, nunca la rechaza, porque la ruta es
            // pública y un 401 aquí cerraría la sesión de quien solo pasaba a mirar.
            Claims publicas = leerClaims(request);
            return chain.filter(publicas == null
                    ? withIdentity(exchange, null, null, null)
                    : withIdentity(exchange, publicas.getSubject(),
                            publicas.get("rol", String.class), barberoIdDe(publicas)));
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Token no proporcionado");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String rol = claims.get("rol", String.class);

            if (!hasRequiredRole(request, rol)) {
                LOGGER.debug("Rol {} sin permiso en {} {}", rol, request.getMethod(), request.getPath());
                return forbidden(exchange);
            }

            return chain.filter(withIdentity(exchange, claims.getSubject(), rol, barberoIdDe(claims)));
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, "Token expirado");
        } catch (JwtException | IllegalArgumentException e) {
            LOGGER.debug("Token rechazado en {} {}: {}",
                    request.getMethod(), request.getPath(), e.getMessage());
            return unauthorized(exchange, "Token inválido");
        }
    }

    /**
     * Se ejecuta antes que los filtros de enrutamiento del gateway, para cortar la petición
     * sin haber contactado al microservicio destino.
     */
    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublic(ServerHttpRequest request) {
        return publicEndpoints.stream().anyMatch(endpoint -> endpoint.matches(request));
    }

    /**
     * Lee los claims si la petición trae un token con firma válida y sin caducar.
     *
     * @return los claims, o {@code null} si no hay token o no es de fiar. Nunca lanza:
     *         quien llama a esto está atendiendo una ruta pública, donde un token malo
     *         significa "anónimo", no "denegado".
     */
    private Claims leerClaims(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(authorization.substring(BEARER_PREFIX.length()).trim())
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** El claim viaja como número JSON; la cabecera es texto. */
    private static String barberoIdDe(Claims claims) {
        Object barberoClaim = claims.get("barberoId");
        return barberoClaim == null ? null : String.valueOf(((Number) barberoClaim).longValue());
    }

    /** Sin regla que aplique, basta con estar autenticado. */
    private boolean hasRequiredRole(ServerHttpRequest request, String rol) {
        return roleRules.stream()
                .filter(rule -> rule.endpoint().matches(request))
                .allMatch(rule -> rule.roles().contains(rol));
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange,
                                           String usuario, String rol, String barberoId) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_HEADER);
                    headers.remove(ROLE_HEADER);
                    headers.remove(BARBERO_HEADER);
                    if (usuario != null) {
                        headers.set(USER_HEADER, usuario);
                    }
                    if (rol != null) {
                        headers.set(ROLE_HEADER, rol);
                    }
                    if (barberoId != null) {
                        headers.set(BARBERO_HEADER, barberoId);
                    }
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String mensaje) {
        return escribirError(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", mensaje);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        return escribirError(exchange, HttpStatus.FORBIDDEN, "Forbidden",
                "Tu rol no tiene permiso para esta operación");
    }

    private Mono<Void> escribirError(ServerWebExchange exchange, HttpStatus status,
                                     String error, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"status\":" + status.value() + ",\"error\":\"" + error
                + "\",\"mensaje\":\"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private static HttpMethod resolveMethod(String method) {
        if (method == null || method.isBlank() || ANY_METHOD.equals(method.trim())) {
            return null;
        }
        return HttpMethod.valueOf(method.trim().toUpperCase());
    }

    /**
     * @param method método exigido, o {@code null} si la ruta aplica a cualquier método.
     */
    private record EndpointMatcher(HttpMethod method, PathPattern pattern) {

        boolean matches(ServerHttpRequest request) {
            if (method != null && !method.equals(request.getMethod())) {
                return false;
            }
            return pattern.matches(request.getPath().pathWithinApplication());
        }
    }

    private record RoleMatcher(EndpointMatcher endpoint, List<String> roles) {
    }
}
