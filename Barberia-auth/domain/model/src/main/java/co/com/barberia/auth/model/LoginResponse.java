package co.com.barberia.auth.model;

public class LoginResponse {
    private String token;
    private String username;
    private String rol;
    private String nombre;
    /** Solo viene informado si rol = BARBERO. El frontend lo usa para pedir su propia ficha. */
    private Long barberoId;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String rol, String nombre, Long barberoId) {
        this.token = token;
        this.username = username;
        this.rol = rol;
        this.nombre = nombre;
        this.barberoId = barberoId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getBarberoId() {
        return barberoId;
    }

    public void setBarberoId(Long barberoId) {
        this.barberoId = barberoId;
    }
}
