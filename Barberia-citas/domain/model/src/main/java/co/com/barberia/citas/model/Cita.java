package co.com.barberia.citas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_nombre", nullable = false)
    private String clienteNombre;

    @Column(name = "cliente_telefono", nullable = false)
    private String clienteTelefono;

    /**
     * Username de la cuenta CLIENTE que reservo, o null si la reserva se hizo sin
     * iniciar sesion. Es la columna por la que un cliente ve su historial.
     *
     * <p>Tiene que ser anulable porque la landing sigue permitiendo reservar como
     * invitado: exigir cuenta para pedir turno perderia reservas. Quien reserva sin
     * cuenta simplemente no puede consultar ni cancelar por la web.
     *
     * <p>Lo rellena el servidor con la cabecera X-Auth-User que pone el gateway,
     * nunca el cuerpo de la peticion. Si viniera del cliente, cualquiera podria
     * apropiarse de las citas de otro mandando su nombre de usuario, o llenarle el
     * historial de citas falsas.
     */
    @Column(name = "cliente_username")
    private String clienteUsername;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    /**
     * Id del barbero asignado. Es la columna por la que se filtra la agenda: un
     * usuario con rol BARBERO solo puede ver y tocar las citas cuyo barberoId
     * coincide con el suyo.
     *
     * <p>Antes solo existia el nombre en texto ('barbero'), que no sirve como
     * criterio de permisos: dos barberos pueden llamarse igual y un cambio de
     * nombre dejaria huerfanas las citas.
     */
    @Column(name = "barbero_id")
    private Long barberoId;

    /** Nombre del barbero copiado al crear la cita, para mostrarlo sin consultar otro servicio. */
    private String barbero;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Comentario del cliente al reservar ("Referencia: skin fade bajo"). */
    @Column(length = 500)
    private String notas;

    /** PENDIENTE, CONFIRMADA, COMPLETADA, CANCELADA */
    private String estado;
}
