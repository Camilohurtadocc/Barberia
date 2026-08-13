package co.com.barberia.servicios.controller;

import co.com.barberia.servicios.model.ConfiguracionSitio;
import co.com.barberia.servicios.repository.ConfiguracionSitioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contenido editable de la pagina principal: portada, textos, y los mensajes de la
 * cinta animada.
 *
 * <p>Leer es publico (lo necesita la landing de cualquier visitante); escribir es
 * solo del ADMIN.
 */
@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String ADMIN = "ADMIN";
    /** La tabla tiene una sola fila; este es su id fijo. */
    private static final Long ID_UNICO = 1L;

    private final ConfiguracionSitioRepository repositorio;

    public ConfiguracionController(ConfiguracionSitioRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ConfiguracionSitio obtener() {
        // Si aun no existe se devuelve una vacia en vez de 404: la landing debe poder
        // pintarse con sus valores por defecto aunque nadie haya configurado nada.
        return repositorio.findById(ID_UNICO).orElseGet(() -> {
            ConfiguracionSitio vacia = new ConfiguracionSitio();
            vacia.setId(ID_UNICO);
            return vacia;
        });
    }

    @PutMapping
    public ResponseEntity<?> actualizar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                        @RequestBody ConfiguracionSitio datos) {
        if (!ADMIN.equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Solo el administrador puede editar la página principal"));
        }
        // El id se fuerza: da igual lo que mande el cliente, siempre es la misma fila.
        datos.setId(ID_UNICO);
        return ResponseEntity.ok(repositorio.save(datos));
    }
}
