package co.com.barberia.servicios.controller;

import co.com.barberia.servicios.model.Servicio;
import co.com.barberia.servicios.repository.ServicioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reglas de acceso: leer es publico (la landing muestra el catalogo sin iniciar
 * sesion); crear, editar y eliminar es exclusivo del ADMIN. Un barbero no define
 * el catalogo ni los precios de la barberia.
 */
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String ADMIN = "ADMIN";

    private final ServicioRepository repositorio;

    public ServicioController(ServicioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<Servicio> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Servicio> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                   @RequestBody Servicio servicio) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        // El id lo asigna la secuencia de la BD. Si el cliente manda uno, JPA lo
        // interpretaria como "actualiza esa fila" y pisaria un registro ajeno.
        servicio.setId(null);
        return ResponseEntity.ok(repositorio.save(servicio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                        @PathVariable Long id,
                                        @RequestBody Servicio datos) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        return repositorio.findById(id)
                .map(existente -> {
                    existente.setNombre(datos.getNombre());
                    existente.setDescripcion(datos.getDescripcion());
                    existente.setPrecio(datos.getPrecio());
                    existente.setDuracionMinutos(datos.getDuracionMinutos());
                    existente.setTag(datos.getTag());
                    return ResponseEntity.ok(repositorio.save(existente));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                      @PathVariable Long id) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        if (!repositorio.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> prohibido() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensaje", "Solo el administrador puede modificar el catálogo"));
    }
}
