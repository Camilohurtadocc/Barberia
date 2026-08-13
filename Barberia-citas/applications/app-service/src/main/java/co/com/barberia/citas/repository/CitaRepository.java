package co.com.barberia.citas.repository;

import co.com.barberia.citas.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data genera la implementacion en tiempo de arranque: no hay que escribirla.
 * La consulta por barbero se deriva del nombre del metodo, sin escribir SQL.
 */
public interface CitaRepository extends JpaRepository<Cita, Long> {

    /** Agenda de un barbero concreto: la base de la regla "cada uno ve lo suyo". */
    List<Cita> findByBarberoIdOrderByFechaHoraAsc(Long barberoId);

    /**
     * Historial de un cliente. Va en orden descendente, al reves que la agenda del
     * barbero: al barbero le interesa lo siguiente que tiene que hacer, y al cliente
     * lo ultimo que reservo.
     */
    List<Cita> findByClienteUsernameOrderByFechaHoraDesc(String clienteUsername);

    List<Cita> findAllByOrderByFechaHoraAsc();
}
