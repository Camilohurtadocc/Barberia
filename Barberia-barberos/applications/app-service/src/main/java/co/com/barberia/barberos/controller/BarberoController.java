package co.com.barberia.barberos.controller;

import co.com.barberia.barberos.model.Barbero;
import co.com.barberia.barberos.repository.BarberoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reglas de acceso:
 * <ul>
 *   <li>Leer: publico (la landing muestra el equipo sin iniciar sesion).</li>
 *   <li>Crear y eliminar: solo ADMIN. Dar de alta o de baja barberos es gestion.</li>
 *   <li>Editar: ADMIN cualquiera; BARBERO unicamente su propia ficha.</li>
 * </ul>
 *
 * <p>La identidad llega en las cabeceras que inyecta el gateway despues de validar
 * la firma del token. El gateway borra las que mande el cliente, asi que no se
 * pueden falsificar desde fuera.
 */
@RestController
@RequestMapping("/api/barberos")
public class BarberoController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String BARBERO_HEADER = "X-Auth-Barbero";

    private static final String ADMIN = "ADMIN";
    private static final String BARBERO = "BARBERO";

    private final BarberoRepository repositorio;

    public BarberoController(BarberoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<Barbero> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Barbero> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                   @RequestBody Barbero barbero) {
        if (!ADMIN.equals(rol)) {
            return prohibido("Solo el administrador puede registrar barberos");
        }
        // El id lo asigna la secuencia de la BD. Si el cliente manda uno, JPA lo
        // interpretaria como "actualiza esa fila" y pisaria un registro ajeno.
        barbero.setId(null);
        if (barbero.getActivo() == null) {
            barbero.setActivo(true);
        }
        return ResponseEntity.ok(repositorio.save(barbero));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                        @RequestHeader(value = BARBERO_HEADER, required = false) Long barberoId,
                                        @PathVariable Long id,
                                        @RequestBody Barbero datos) {
        // Un barbero solo puede tocar SU ficha: se compara el id de la ruta contra el
        // que venia firmado en el token, no contra nada del cuerpo de la peticion.
        if (BARBERO.equals(rol) && !id.equals(barberoId)) {
            return prohibido("Solo puedes modificar tu propia información");
        }
        if (!ADMIN.equals(rol) && !BARBERO.equals(rol)) {
            return prohibido("Necesitas iniciar sesión para editar");
        }

        return repositorio.findById(id)
                .map(existente -> {
                    existente.setNombre(datos.getNombre());
                    existente.setNombreCorto(datos.getNombreCorto());
                    existente.setCargo(datos.getCargo());
                    existente.setEspecialidad(datos.getEspecialidad());
                    existente.setAniosExperiencia(datos.getAniosExperiencia());
                    existente.setCortes(datos.getCortes());
                    existente.setColor(datos.getColor());
                    existente.setInstagram(datos.getInstagram());
                    existente.setFacebook(datos.getFacebook());
                    existente.setFotoUrl(datos.getFotoUrl());
                    if (datos.getSlots() != null) {
                        existente.setSlots(datos.getSlots());
                    }
                    if (datos.getGaleria() != null) {
                        existente.setGaleria(datos.getGaleria());
                    }
                    // Dar de baja a un barbero es gestion: un barbero no puede
                    // reactivarse ni desactivarse a si mismo.
                    if (ADMIN.equals(rol) && datos.getActivo() != null) {
                        existente.setActivo(datos.getActivo());
                    }
                    return ResponseEntity.ok(repositorio.save(existente));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                      @PathVariable Long id) {
        if (!ADMIN.equals(rol)) {
            return prohibido("Solo el administrador puede eliminar barberos");
        }
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> prohibido(String mensaje) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", mensaje));
    }
}
