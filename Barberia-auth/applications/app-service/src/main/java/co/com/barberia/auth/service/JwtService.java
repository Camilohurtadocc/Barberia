package co.com.barberia.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET = "barberiaSecretKey1234567890123456789012345678901234567890";
    private static final long EXPIRATION_TIME = 86400000; // 24 horas

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * Mete el rol y el barberoId dentro del token firmado.
     *
     * <p>Que el barberoId viaje firmado es lo que hace segura la regla "cada barbero
     * solo ve su agenda": el cliente no puede cambiarlo sin invalidar la firma HS256.
     * Si se pasara como cabecera o parametro, bastaria con editarlo para ver la
     * agenda de otro.
     *
     * @param barberoId null para cuentas ADMIN.
     */
    public String generateToken(String username, String rol, Long barberoId) {
        var builder = Jwts.builder()
                .setSubject(username)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME));

        if (barberoId != null) {
            builder.claim("barberoId", barberoId);
        }
        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    public String extractRol(String token) {
        return (String) validateToken(token).get("rol");
    }

    public Long extractBarberoId(String token) {
        Object valor = validateToken(token).get("barberoId");
        return valor == null ? null : ((Number) valor).longValue();
    }
}
