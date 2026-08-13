package co.com.barberia.servicios.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "servicios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private Double precio;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    /**
     * Etiqueta destacada de la tarjeta: BESTSELLER, TRENDING, PREMIUM, RITUAL.
     * Va null cuando el servicio no lleva distintivo.
     */
    private String tag;
}
