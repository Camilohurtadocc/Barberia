package co.com.barberia.servicios.controller;

import co.com.barberia.servicios.service.AlmacenArchivos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Subida y descarga de imagenes.
 *
 * <p>Subir esta reservado a ADMIN y BARBERO (el barbero necesita cambiar su propia
 * foto y su galeria). Descargar es publico: son las fotos que enseña la landing a
 * cualquier visitante.
 */
@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    private static final String ROLE_HEADER = "X-Auth-Role";
    private static final String ADMIN = "ADMIN";
    private static final String BARBERO = "BARBERO";

    private final AlmacenArchivos almacen;

    public ArchivoController(AlmacenArchivos almacen) {
        this.almacen = almacen;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subir(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                   @RequestParam("archivo") MultipartFile archivo) {
        if (!ADMIN.equals(rol) && !BARBERO.equals(rol)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Necesitas iniciar sesión para subir imágenes"));
        }
        try {
            String nombre = almacen.guardar(archivo);
            // Se devuelve la ruta lista para usar como src, no solo el nombre: asi el
            // frontend no tiene que saber como se construye la URL de descarga.
            return ResponseEntity.ok(Map.of(
                    "nombre", nombre,
                    "url", "/api/archivos/" + nombre));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensaje", "No se pudo guardar la imagen"));
        }
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        try {
            Path ruta = almacen.resolver(nombre);
            if (!Files.exists(ruta) || !Files.isRegularFile(ruta)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    // El nombre lleva un UUID, asi que un archivo distinto tiene siempre
                    // otra URL: se puede cachear sin miedo a servir una foto vieja.
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                    .contentType(MediaType.parseMediaType(almacen.tipoDeContenido(nombre)))
                    .body(new FileSystemResource(ruta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
