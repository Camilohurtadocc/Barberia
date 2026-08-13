package co.com.barberia.barberos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "barberos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barbero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre completo: "Marcos Villegas". */
    @Column(nullable = false)
    private String nombre;

    /** Nombre corto para las tarjetas: "MARCOS V.". */
    @Column(name = "nombre_corto")
    private String nombreCorto;

    /** Cargo mostrado bajo el nombre: Master Barber, Senior Barber, Barber Artist. */
    private String cargo;

    private String especialidad;

    @Column(name = "anios_experiencia")
    private Integer aniosExperiencia;

    /** Texto libre porque se muestra abreviado ("8.4K"), no se opera con el. */
    private String cortes;

    /** Color de acento del barbero en la UI, en hexadecimal (#ff3cac). */
    private String color;

    private String instagram;
    private String facebook;

    @Column(name = "foto_url")
    private String fotoUrl;

    /** Un barbero inactivo deja de ofrecerse en la landing pero conserva su historial. */
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Franjas horarias que ofrece este barbero ("09:00", "13:00"...).
     *
     * <p>@ElementCollection crea la tabla aparte barbero_slots (barbero_id, slot).
     * Se prefiere a guardar un CSV en una columna porque asi se puede consultar y
     * ordenar desde SQL, y se ve como tabla real en pgAdmin.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "barbero_slots", joinColumns = @JoinColumn(name = "barbero_id"))
    @Column(name = "slot")
    private List<String> slots = new ArrayList<>();

    /** URLs de fotos de trabajos. Misma decision que slots: tabla aparte. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "barbero_galeria", joinColumns = @JoinColumn(name = "barbero_id"))
    @Column(name = "url")
    private List<String> galeria = new ArrayList<>();
}
