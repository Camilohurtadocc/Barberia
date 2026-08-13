package co.com.barberia.servicios.repository;

import co.com.barberia.servicios.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data genera la implementacion en tiempo de arranque: no hay que escribirla.
 * Se detecta solo porque este paquete cuelga de co.com.barberia.servicios, el mismo
 * de MainApplication.
 */
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
}
