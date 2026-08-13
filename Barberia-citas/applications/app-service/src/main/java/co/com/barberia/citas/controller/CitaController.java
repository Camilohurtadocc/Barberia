package co.com.barberia.citas.controller;

import co.com.barberia.citas.model.Cita;
import co.com.barberia.citas.repository.CitaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Reglas de acceso:
 * <ul>
 *   <li>Crear: publico. Es el formulario de reserva de la landing. Si quien reserva
 *       tiene sesion, la cita queda ligada a su cuenta.</li>
 *   <li>Listar y ver: ADMIN ve todas; BARBERO las de su agenda; CLIENTE las suyas.</li>
 *   <li>Cambiar estado: ADMIN cualquiera; BARBERO las suyas; CLIENTE solo para
 *       CANCELAR una cita propia que aun no haya pasado.</li>
 *   <li>Eliminar: solo ADMIN. Ni barbero ni cliente borran: cancelan cambiando el
 *       estado, para que no se pierda el historial.</li>
 * </ul>
 *
 * <p>Ni el barberoId ni el username se leen del cuerpo ni de un parametro: llegan en
 * las cabeceras que pone el gateway a partir de los claims firmados del token. Si se
 * leyeran del cliente, cualquiera podria pedir la agenda de otro cambiando un numero
 * o consultar el historial ajeno escribiendo su nombre de usuario.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String BARBERO_HEADER = "X-Auth-Barbero";
    private static final String USER_HEADER = "X-Auth-User";

    private static final String ADMIN = "ADMIN";
    private static final String BARBERO = "BARBERO";
    private static final String CLIENTE = "CLIENTE";

    private static final String CANCELADA = "CANCELADA";

    private final CitaRepository repositorio;

    public CitaController(CitaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                    @RequestHeader(value = BARBERO_HEADER, required = false) Long barberoId,
                                    @RequestHeader(value = USER_HEADER, required = false) String username) {
        if (ADMIN.equals(rol)) {
            return ResponseEntity.ok(repositorio.findAllByOrderByFechaHoraAsc());
        }
        if (BARBERO.equals(rol)) {
            if (barberoId == null) {
                // Cuenta de barbero sin ficha vinculada: mejor lista vacia que todas.
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(repositorio.findByBarberoIdOrderByFechaHoraAsc(barberoId));
        }
        if (CLIENTE.equals(rol)) {
            if (username == null || username.isBlank()) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(repositorio.findByClienteUsernameOrderByFechaHoraDesc(username));
        }
        return prohibido("Necesitas iniciar sesión para ver las citas");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                          @RequestHeader(value = BARBERO_HEADER, required = false) Long barberoId,
                                          @RequestHeader(value = USER_HEADER, required = false) String username,
                                          @PathVariable Long id) {
        Cita cita = repositorio.findById(id).orElse(null);
        if (cita == null) {
            return ResponseEntity.notFound().build();
        }
        if (ADMIN.equals(rol)
                || (BARBERO.equals(rol) && esSuya(cita, barberoId))
                || (CLIENTE.equals(rol) && esDelCliente(cita, username))) {
            return ResponseEntity.ok(cita);
        }
        return prohibido("Esta cita no pertenece a tu agenda");
    }

    @PostMapping
    public Cita crear(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                      @RequestHeader(value = USER_HEADER, required = false) String username,
                      @RequestBody Cita cita) {
        // El id lo asigna la secuencia de la BD. Si el cliente manda uno, JPA lo
        // interpretaria como "actualiza esa fila" y pisaria una cita ajena.
        cita.setId(null);
        // El estado inicial lo decide el servidor: el formulario publico de la
        // landing no puede crear una cita ya CONFIRMADA.
        cita.setEstado("PENDIENTE");

        // Se ignora por completo el clienteUsername que venga en el cuerpo y se pone
        // el del token, o null si la reserva es anonima. Aceptar el del cuerpo dejaria
        // que cualquiera metiera citas en el historial de otra persona.
        cita.setClienteUsername(CLIENTE.equals(rol) && username != null && !username.isBlank()
                ? username
                : null);

        return repositorio.save(cita);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                           @RequestHeader(value = BARBERO_HEADER, required = false) Long barberoId,
                                           @RequestHeader(value = USER_HEADER, required = false) String username,
                                           @PathVariable Long id,
                                           @RequestParam String estado) {
        Cita cita = repositorio.findById(id).orElse(null);
        if (cita == null) {
            return ResponseEntity.notFound().build();
        }

        // El cliente tiene un permiso mucho mas estrecho que los otros dos roles:
        // solo puede CANCELAR, y solo una cita suya que todavia no haya pasado. Sin
        // el filtro de estado podria "descancelar" una cita que el barbero anulo, o
        // borrar del historial una que ya se le cobro.
        if (CLIENTE.equals(rol)) {
            if (!esDelCliente(cita, username)) {
                return prohibido("Esta cita no es tuya");
            }
            if (!CANCELADA.equalsIgnoreCase(estado)) {
                return prohibido("Como cliente solo puedes cancelar una cita");
            }
            if (cita.getFechaHora() != null && cita.getFechaHora().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("mensaje", "No se puede cancelar una cita que ya pasó"));
            }
            if (CANCELADA.equalsIgnoreCase(cita.getEstado())) {
                return ResponseEntity.ok(cita);
            }
            cita.setEstado(CANCELADA);
            return ResponseEntity.ok(repositorio.save(cita));
        }

        if (!ADMIN.equals(rol) && !(BARBERO.equals(rol) && esSuya(cita, barberoId))) {
            return prohibido("Solo puedes gestionar las citas de tu agenda");
        }
        cita.setEstado(estado);
        return ResponseEntity.ok(repositorio.save(cita));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                      @PathVariable Long id) {
        if (!ADMIN.equals(rol)) {
            return prohibido("Solo el administrador puede eliminar citas");
        }
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esSuya(Cita cita, Long barberoId) {
        return barberoId != null && barberoId.equals(cita.getBarberoId());
    }

    /**
     * Comparacion sin distinguir mayusculas, igual que el login. Los usuarios se
     * guardan en minusculas, pero el claim del token viene tal cual se emitio y una
     * cita antigua podria tener otra capitalizacion.
     */
    private boolean esDelCliente(Cita cita, String username) {
        return username != null && !username.isBlank()
                && cita.getClienteUsername() != null
                && cita.getClienteUsername().equalsIgnoreCase(username);
    }

    private ResponseEntity<?> prohibido(String mensaje) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", mensaje));
    }
}
