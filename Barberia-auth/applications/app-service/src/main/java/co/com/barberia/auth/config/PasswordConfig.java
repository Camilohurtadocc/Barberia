package co.com.barberia.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cifrado de contraseñas con BCrypt.
 *
 * <p>BCrypt es un hash de un solo sentido: de la contraseña se obtiene el hash, pero del
 * hash no se puede volver a la contraseña. Por eso el sistema nunca puede "recordarle" su
 * clave a nadie, solo permitir cambiarla, y por eso ni el administrador puede leerlas.
 *
 * <p>Incorpora un <em>salt</em> aleatorio en cada hash, así que dos usuarios con la misma
 * contraseña producen hashes distintos y las tablas precalculadas (rainbow tables) no
 * sirven. El factor de coste 10 (por defecto) hace que verificar sea deliberadamente lento,
 * lo que encarece la fuerza bruta sin que se note en un login normal.
 *
 * <p>Nota: esto es independiente del JWT. El token firma <em>quién eres</em> después de
 * entrar; BCrypt protege <em>cómo se guarda</em> la clave con la que entras.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
