package co.com.barberia.servicios.repository;

import co.com.barberia.servicios.model.ConfiguracionSitio;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tabla de una sola fila (id = 1) con el contenido editable de la portada. */
public interface ConfiguracionSitioRepository extends JpaRepository<ConfiguracionSitio, Long> {
}
