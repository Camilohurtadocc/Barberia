package co.com.barberia.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    /** ADMIN, BARBERO o CLIENTE. Viaja dentro del JWT y el gateway lo usa para autorizar. */
    private String rol;

    /** Nombre para mostrar en el panel; no interviene en la autenticacion. */
    private String nombre;

    /**
     * Correo del cliente. Opcional y solo se llena en las cuentas CLIENTE, que se
     * crean solas desde /auth/registro.
     *
     * <p>No lleva unique = true a proposito. El identificador de la cuenta es
     * 'username'; el correo es un dato de contacto mas. Marcarlo unico obligaria a
     * tener uno por cuenta y romperia el alta de barberos y admins, que no lo piden.
     */
    private String email;

    /**
     * Telefono de contacto del cliente, en el mismo formato que clienteTelefono de
     * una cita. Sirve para rellenar el formulario de reserva sin volver a teclearlo.
     */
    private String telefono;

    /**
     * Id del barbero de barberia_barberos al que pertenece esta cuenta. Solo se llena
     * cuando rol = BARBERO; en las cuentas ADMIN va null.
     *
     * <p>Es un Long suelto y NO una clave foranea: la tabla barberos vive en otra base
     * de datos (patron database-per-service), asi que el motor no puede validarlo. La
     * coherencia la mantiene la aplicacion, igual que citas.servicio_id.
     *
     * <p>De aqui sale el claim 'barberoId' del token, que es lo que permite filtrar la
     * agenda para que cada barbero vea solo sus citas.
     */
    @Column(name = "barbero_id")
    private Long barberoId;
}
