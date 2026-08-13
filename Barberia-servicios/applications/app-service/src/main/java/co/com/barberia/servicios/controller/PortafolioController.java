package co.com.barberia.servicios.controller;

import co.com.barberia.servicios.model.ItemPortafolio;
import co.com.barberia.servicios.repository.PortafolioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Galeria de trabajos de la pagina principal. Leer es publico; gestionarla es del ADMIN.
 */
@RestController
@RequestMapping("/api/portafolio")
public class PortafolioController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String ADMIN = "ADMIN";

    private final PortafolioRepository repositorio;

    public PortafolioController(PortafolioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<ItemPortafolio> listar() {
        return repositorio.findAllByOrderByOrdenAscIdAsc();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                   @RequestBody ItemPortafolio item) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        // El id lo asigna la secuencia de la BD. Si el cliente manda uno, JPA lo
        // interpretaria como "actualiza esa fila" y pisaria un registro ajeno.
        item.setId(null);
        if (item.getOrden() == null) {
            item.setOrden(repositorio.findAll().size() + 1);
        }
        return ResponseEntity.ok(repositorio.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                        @PathVariable Long id,
                                        @RequestBody ItemPortafolio datos) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        return repositorio.findById(id)
                .map(existente -> {
                    existente.setTitulo(datos.getTitulo());
                    existente.setCategoria(datos.getCategoria());
                    existente.setImagenUrl(datos.getImagenUrl());
                    existente.setOrden(datos.getOrden());
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
                .body(Map.of("mensaje", "Solo el administrador puede gestionar el portafolio"));
    }
}
