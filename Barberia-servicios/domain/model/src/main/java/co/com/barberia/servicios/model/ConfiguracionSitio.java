package co.com.barberia.servicios.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido editable de la pagina principal: lo que el administrador puede cambiar
 * sin tocar codigo ni reconstruir el frontend.
 *
 * <p>Es una tabla de UNA sola fila (id = 1). Se modela como entidad y no como
 * pares clave/valor porque cada dato tiene su propio tipo y significado: asi el
 * frontend recibe un objeto con campos con nombre en vez de tener que buscar
 * claves sueltas y convertirlas a mano.
 */
@Entity
@Table(name = "configuracion_sitio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionSitio {

    @Id
    private Long id;

    // --- Portada ---
    @Column(name = "hero_titulo")
    private String heroTitulo;

    @Column(name = "hero_subtitulo", length = 500)
    private String heroSubtitulo;

    /** Imagen grande de la portada. Puede ser una URL externa o /api/archivos/xxx.jpg */
    @Column(name = "hero_imagen_url", length = 500)
    private String heroImagenUrl;

    // --- Bloque "sobre nosotros" ---
    @Column(name = "sobre_imagen_url", length = 500)
    private String sobreImagenUrl;

    @Column(name = "sobre_texto", length = 1000)
    private String sobreTexto;

    // --- Datos de contacto que salen en la landing ---
    private String direccion;
    private String telefono;
    private String instagram;

    /**
     * Frases de la cinta animada. Se muestran encadenadas y separadas por un punto medio.
     *
     * <p>Tabla aparte (@ElementCollection) en vez de un texto con separadores: asi el
     * administrador las edita como lista y ningun mensaje que contenga el separador
     * parte la frase en dos.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ticker_mensajes", joinColumns = @JoinColumn(name = "configuracion_id"))
    @Column(name = "mensaje", length = 300)
    private List<String> tickerMensajes = new ArrayList<>();
}
