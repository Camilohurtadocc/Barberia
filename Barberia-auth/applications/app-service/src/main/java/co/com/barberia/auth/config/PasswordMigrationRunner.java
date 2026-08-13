package co.com.barberia.auth.config;

import co.com.barberia.auth.model.Usuario;
import co.com.barberia.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte a BCrypt las contraseñas que aún estén en texto plano.
 *
 * <p>Hace falta porque las cuentas iniciales nacen en {@code data.sql}, y un script SQL no
 * puede generar un hash BCrypt: el algoritmo vive en la aplicación. La alternativa sería
 * pegar hashes precalculados en el SQL, pero entonces nadie podría leer ni cambiar las
 * claves de ejemplo sin volver a calcularlos a mano.
 *
 * <p>Se ejecuta en cada arranque y es idempotente: reconoce los hashes ya convertidos por
 * su prefijo ({@code $2a$}, {@code $2b$}, {@code $2y$}) y los deja intactos. Volver a
 * cifrar un hash lo destruiría, porque el resultado ya no coincidiría con la contraseña
 * original.
 */
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UsuarioRepository repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<Usuario> pendientes = repositorio.findAll().stream()
                .filter(u -> u.getPassword() != null && !esBcrypt(u.getPassword()))
                .toList();

        if (pendientes.isEmpty()) {
            return;
        }

        pendientes.forEach(u -> u.setPassword(passwordEncoder.encode(u.getPassword())));
        repositorio.saveAll(pendientes);
        LOGGER.info("Contraseñas cifradas con BCrypt: {} cuenta(s) migradas", pendientes.size());
    }

    private boolean esBcrypt(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
