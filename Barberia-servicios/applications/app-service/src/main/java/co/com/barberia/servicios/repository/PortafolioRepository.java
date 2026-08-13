package co.com.barberia.servicios.repository;

import co.com.barberia.servicios.model.ItemPortafolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortafolioRepository extends JpaRepository<ItemPortafolio, Long> {

    /** La galería se muestra en el orden que fija el administrador, no por id. */
    List<ItemPortafolio> findAllByOrderByOrdenAscIdAsc();
}
