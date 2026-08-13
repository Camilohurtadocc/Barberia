package co.com.barberia.barberos.repository;

import co.com.barberia.barberos.model.Barbero;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data genera la implementacion en tiempo de arranque: no hay que escribirla.
 * Se detecta solo porque este paquete cuelga de co.com.barberia.barberos, el mismo
 * de MainApplication.
 */
public interface BarberoRepository extends JpaRepository<Barbero, Long> {
}
