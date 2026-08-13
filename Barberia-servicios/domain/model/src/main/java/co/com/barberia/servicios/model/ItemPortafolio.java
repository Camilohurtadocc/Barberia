package co.com.barberia.servicios.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cada foto de la galeria de trabajos de la pagina principal.
 *
 * <p>Entidad propia y no una lista dentro de {@link ConfiguracionSitio} porque cada
 * item tiene titulo, categoria y orden: es un registro con estructura, no un dato
 * suelto, y el administrador los da de alta y de baja uno a uno.
 */
@Entity
@Table(name = "portafolio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPortafolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    /** Etiqueta en mayusculas: FADE, CLASSIC, LIFESTYLE, PRECISION, BEARD. */
    private String categoria;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /** Posicion en la galeria. Sin esto el orden dependeria del id y no se podria reordenar. */
    private Integer orden;
}
