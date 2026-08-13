package co.com.barberia.auth.repository;

import co.com.barberia.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data genera la implementacion en tiempo de arranque: no hay que escribirla.
 * Las consultas se derivan del nombre del metodo, sin escribir SQL.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    /**
     * Busqueda sin distinguir mayusculas, que es la que usa el login.
     *
     * <p>Postgres compara texto respetando mayusculas, asi que 'Marcos' no encuentra
     * a 'marcos'. Los navegadores y teclados moviles capitalizan la primera letra por
     * su cuenta, de modo que el usuario acaba viendo "credenciales invalidas" con la
     * clave correcta y sin nada visible que lo explique.
     */
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    /** Evita que convivan 'marcos' y 'Marcos' como cuentas distintas. */
    boolean existsByUsernameIgnoreCase(String username);

    /** Usado al borrar un barbero: su cuenta de acceso se va con el. */
    List<Usuario> findByBarberoId(Long barberoId);
}
