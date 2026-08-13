package co.com.barberia.servicios.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda y recupera las imagenes que sube el administrador desde el explorador de archivos.
 *
 * <p>Los archivos van a un directorio montado como volumen de Docker, NO dentro de la
 * imagen: lo que se escribe en el sistema de archivos de un contenedor desaparece al
 * reconstruirlo, y las fotos subidas se perderian en cada despliegue.
 */
@Service
public class AlmacenArchivos {

    /** Tipos permitidos y la extension con la que se guarda cada uno. */
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "image/avif", ".avif"
    );

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024; // 5 MB

    private final Path directorio;

    public AlmacenArchivos(@Value("${barberia.uploads.dir:/app/uploads}") String ruta) {
        this.directorio = Paths.get(ruta).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepararDirectorio() throws IOException {
        Files.createDirectories(directorio);
    }

    /**
     * @return el nombre con el que quedo guardado el archivo.
     * @throws IllegalArgumentException si el archivo no es una imagen admitida o pesa de mas.
     */
    public String guardar(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No llegó ningún archivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("La imagen supera los 5 MB");
        }

        String tipo = archivo.getContentType() == null
                ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        String extension = TIPOS_PERMITIDOS.get(tipo);
        if (extension == null) {
            throw new IllegalArgumentException("Formato no admitido. Usa JPG, PNG, WEBP, GIF o AVIF");
        }

        // El nombre se genera aqui y NUNCA se reutiliza el que manda el cliente.
        // Un nombre como "../../etc/passwd" o "app.jar" escribiria fuera del
        // directorio o pisaria un archivo del sistema. Con un UUID tampoco hay
        // colisiones entre dos usuarios que suban "foto.jpg" a la vez.
        String nombre = UUID.randomUUID() + extension;
        Path destino = directorio.resolve(nombre);

        try (var entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return nombre;
    }

    /**
     * Resuelve un nombre a su ruta en disco.
     *
     * @throws IllegalArgumentException si el nombre intenta salir del directorio.
     */
    public Path resolver(String nombre) {
        Path candidato = directorio.resolve(nombre).normalize();
        // Segunda barrera contra el path traversal: aunque el nombre venga de la URL
        // y no del cliente que subio, "..%2F..%2Fapp.jar" seguiria siendo un nombre
        // valido para resolve(). Se comprueba que el resultado siga dentro.
        if (!candidato.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        return candidato;
    }

    public String tipoDeContenido(String nombre) {
        String n = nombre.toLowerCase(Locale.ROOT);
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".avif")) return "image/avif";
        return "image/jpeg";
    }
}
