package co.com.barberia.auth.controller;

import co.com.barberia.auth.model.LoginRequest;
import co.com.barberia.auth.model.LoginResponse;
import co.com.barberia.auth.model.Usuario;
import co.com.barberia.auth.repository.UsuarioRepository;
import co.com.barberia.auth.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Cabeceras que inyecta el gateway tras validar el token. El cliente no puede falsificarlas. */
    private static final String USER_HEADER = "X-Auth-User";
    private static final String ROLE_HEADER = "X-Auth-Role";

    private static final String ADMIN = "ADMIN";
    private static final String BARBERO = "BARBERO";
    /** Cuenta que se registra sola desde la web para reservar y ver su historial. */
    private static final String CLIENTE = "CLIENTE";
    private static final Set<String> ROLES_VALIDOS = Set.of(ADMIN, BARBERO, CLIENTE);

    private final JwtService jwtService;
    private final UsuarioRepository repositorio;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService,
                          UsuarioRepository repositorio,
                          PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================================
    // Autenticacion
    // =========================================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // matches() vuelve a cifrar lo que llega usando el salt guardado dentro del hash
        // y compara los resultados. Nunca se descifra lo almacenado: BCrypt no tiene vuelta.
        // IgnoreCase: el nombre de usuario no distingue mayusculas. La contrasena SI,
        // porque ahi la variedad de caracteres es justamente lo que la hace fuerte.
        Usuario usuario = repositorio.findByUsernameIgnoreCase(nombreLimpio(request.getUsername()))
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElse(null);

        if (usuario == null) {
            // Mismo mensaje si el usuario no existe o si la clave falla: distinguirlos
            // permitiria averiguar que cuentas existen probando nombres.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Credenciales inválidas"));
        }

        String token = jwtService.generateToken(
                usuario.getUsername(), usuario.getRol(), usuario.getBarberoId());

        return ResponseEntity.ok(new LoginResponse(
                token, usuario.getUsername(), usuario.getRol(),
                usuario.getNombre(), usuario.getBarberoId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    /**
     * Alta de un cliente desde la web. Es el UNICO endpoint que crea cuentas sin
     * estar autenticado, y por eso fuerza el rol en el servidor.
     *
     * <p>El rol NO se lee del cuerpo. Si se leyera, bastaria con mandar
     * {"rol":"ADMIN"} en el registro para nombrarse administrador: el gateway deja
     * pasar esta ruta sin token, asi que aqui no hay nadie por encima comprobando
     * nada. Lo mismo con barberoId, que se anula para que una cuenta recien
     * registrada no pueda reclamar la agenda de un barbero.
     *
     * <p>Devuelve el token ya emitido para no obligar a iniciar sesion justo
     * despues de registrarse.
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody Usuario datos) {
        if (esVacio(datos.getUsername()) || esVacio(datos.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "Usuario y contraseña son obligatorios"));
        }
        if (datos.getPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "La contraseña debe tener al menos 6 caracteres"));
        }

        String username = nombreLimpio(datos.getUsername());
        if (repositorio.existsByUsernameIgnoreCase(username)) {
            // 409 y no 400: el formulario esta bien relleno, lo que pasa es que el
            // nombre esta cogido. El frontend lo distingue para senalar solo ese campo.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Ese usuario ya está registrado"));
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsername(username);
        nuevo.setPassword(passwordEncoder.encode(datos.getPassword()));
        nuevo.setRol(CLIENTE);
        nuevo.setBarberoId(null);
        nuevo.setNombre(esVacio(datos.getNombre()) ? username : datos.getNombre().trim());
        nuevo.setEmail(datos.getEmail() == null ? null : datos.getEmail().trim());
        nuevo.setTelefono(datos.getTelefono() == null ? null : datos.getTelefono().trim());

        Usuario guardado = repositorio.save(nuevo);
        String token = jwtService.generateToken(
                guardado.getUsername(), guardado.getRol(), guardado.getBarberoId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(
                token, guardado.getUsername(), guardado.getRol(),
                guardado.getNombre(), guardado.getBarberoId()));
    }

    /**
     * Datos de la cuenta que esta usando la sesion. El frontend los usa para
     * rellenar el formulario de reserva con el nombre y el telefono del cliente.
     *
     * <p>Se identifica por la cabecera del gateway, nunca por un id de la peticion:
     * asi nadie puede pedir el perfil de otra persona.
     */
    @GetMapping("/perfil")
    public ResponseEntity<?> perfil(@RequestHeader(value = USER_HEADER, required = false) String username) {
        if (esVacio(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No autenticado"));
        }
        return repositorio.findByUsername(username)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(sinPassword(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Actualiza nombre, email y telefono de la propia cuenta. */
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestHeader(value = USER_HEADER, required = false) String username,
                                              @RequestBody Usuario datos) {
        if (esVacio(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No autenticado"));
        }
        Usuario usuario = repositorio.findByUsername(username).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        // Solo se tocan los campos de contacto. Cambiar aqui rol, barberoId,
        // username o password permitiria a cualquiera ascenderse desde su perfil.
        if (!esVacio(datos.getNombre())) {
            usuario.setNombre(datos.getNombre().trim());
        }
        usuario.setEmail(datos.getEmail() == null ? null : datos.getEmail().trim());
        usuario.setTelefono(datos.getTelefono() == null ? null : datos.getTelefono().trim());
        return ResponseEntity.ok(sinPassword(repositorio.save(usuario)));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Token no proporcionado"));
        }
        try {
            String token = authorization.substring(7);
            return ResponseEntity.ok(Map.of(
                    "username", jwtService.extractUsername(token),
                    "rol", jwtService.extractRol(token),
                    "valido", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Token inválido"));
        }
    }

    // =========================================================================
    // Gestion de cuentas (solo ADMIN)
    //
    // El gateway ya bloquea estas rutas para quien no sea ADMIN. La comprobacion
    // se repite aqui a proposito: si alguien alcanzara el servicio sin pasar por
    // el gateway, la regla seguiria en pie.
    // =========================================================================

    @GetMapping("/usuarios")
    public ResponseEntity<?> listar(@RequestHeader(value = ROLE_HEADER, required = false) String rol) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        // Nunca se devuelve la contrasena, ni siquiera cifrada: un hash filtrado
        // se puede atacar por fuerza bruta con calma y sin dejar rastro.
        List<Map<String, Object>> salida = repositorio.findAll().stream()
                .map(this::sinPassword)
                .toList();
        return ResponseEntity.ok(salida);
    }

    /**
     * Crea una cuenta de acceso. Sirve tanto para dar de alta a un barbero (rol BARBERO,
     * con el barberoId que devolvio el alta en servicio-barberos) como para nombrar a
     * otro administrador (rol ADMIN, sin barberoId).
     */
    @PostMapping("/usuarios")
    public ResponseEntity<?> crear(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                   @RequestBody Usuario usuario) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        if (esVacio(usuario.getUsername()) || esVacio(usuario.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "username y password son obligatorios"));
        }
        // Se guarda normalizado a minusculas para que no puedan coexistir 'marcos' y
        // 'Marcos': con el login sin distinguir mayusculas, dos cuentas asi serian
        // indistinguibles al entrar y una de las dos quedaria inalcanzable.
        usuario.setUsername(nombreLimpio(usuario.getUsername()));

        if (repositorio.existsByUsernameIgnoreCase(usuario.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "Ya existe un usuario con ese nombre"));
        }

        String rolNuevo = esVacio(usuario.getRol()) ? BARBERO : usuario.getRol().trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rolNuevo)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El rol debe ser ADMIN, BARBERO o CLIENTE"));
        }
        // Solo BARBERO arrastra un barberoId. Ni ADMIN ni CLIENTE representan a un
        // barbero, y si se colara uno el filtro de agenda los trataria como tal,
        // dandoles acceso a las citas de esa ficha.
        if (BARBERO.equals(rolNuevo)) {
            if (usuario.getBarberoId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "Un usuario BARBERO necesita el barberoId de su ficha"));
            }
        } else {
            usuario.setBarberoId(null);
        }

        usuario.setId(null);
        usuario.setRol(rolNuevo);
        // La contrasena se cifra ANTES de tocar la base: en la tabla nunca hay texto plano.
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        return ResponseEntity.ok(sinPassword(repositorio.save(usuario)));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                      @RequestHeader(value = USER_HEADER, required = false) String username,
                                      @PathVariable Long id) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        Usuario usuario = repositorio.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        // Borrarse a uno mismo deja la sesion viva con una cuenta inexistente.
        if (usuario.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "No puedes eliminar tu propia cuenta"));
        }
        // Dejar el sistema sin ningun ADMIN lo volveria inaccesible para siempre.
        if (ADMIN.equals(usuario.getRol()) && contarAdmins() <= 1) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "No se puede eliminar el único administrador"));
        }
        repositorio.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Borra la cuenta asociada a un barbero. La usa el panel al eliminar al barbero. */
    @DeleteMapping("/usuarios/barbero/{barberoId}")
    public ResponseEntity<?> eliminarPorBarbero(@RequestHeader(value = ROLE_HEADER, required = false) String rol,
                                                @PathVariable Long barberoId) {
        if (!ADMIN.equals(rol)) {
            return prohibido();
        }
        repositorio.deleteAll(repositorio.findByBarberoId(barberoId));
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Cuenta propia (cualquier rol autenticado)
    // =========================================================================

    /**
     * Cambio de contrasena propia. Se identifica por la cabecera que puso el gateway,
     * no por un id del cuerpo: asi un barbero no puede cambiarle la clave a otro.
     */
    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@RequestHeader(value = USER_HEADER, required = false) String username,
                                             @RequestBody Map<String, String> cuerpo) {
        if (esVacio(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No autenticado"));
        }
        Usuario usuario = repositorio.findByUsername(username).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        String nueva = cuerpo.get("nueva");
        if (esVacio(nueva)) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La nueva contraseña no puede ir vacía"));
        }
        // Se exige la clave actual aunque la sesion sea valida: si alguien deja el equipo
        // abierto, no deberia poder secuestrarle la cuenta cambiandole la contrasena.
        if (!passwordEncoder.matches(cuerpo.get("actual"), usuario.getPassword())) {
            // 400 y no 401 a proposito: la SESION es valida, lo que falla es un dato
            // del formulario. Con 401 el frontend cerraria la sesion de golpe, que es
            // justo lo contrario de lo que necesita quien se equivoco tecleando.
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "La contraseña actual no coincide"));
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
        repositorio.save(usuario);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada"));
    }

    // =========================================================================

    private long contarAdmins() {
        return repositorio.findAll().stream().filter(u -> ADMIN.equals(u.getRol())).count();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Quita espacios sobrantes (el copiar/pegar suele arrastrarlos) y baja a minusculas. */
    private String nombreLimpio(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    private ResponseEntity<?> prohibido() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("mensaje", "Solo el administrador puede gestionar cuentas"));
    }

    private Map<String, Object> sinPassword(Usuario u) {
        // HashMap y no Map.of() porque nombre y barberoId pueden ser null,
        // y Map.of() lanza NullPointerException con valores nulos.
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("rol", u.getRol());
        m.put("nombre", u.getNombre());
        m.put("barberoId", u.getBarberoId());
        m.put("email", u.getEmail());
        m.put("telefono", u.getTelefono());
        return m;
    }
}
