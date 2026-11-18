package mx.gob.sedif.sea.security;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import mx.gob.sedif.sea.core.catalogo.Sede;
import mx.gob.sedif.sea.util.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Personal administrativo del IBA, del DIF e informatica.
 *
 * <p>La ciudadania NO tiene cuenta: el registro publico es anonimo por diseno.
 * Sus datos viven en {@code persona}, que no es una tabla de acceso.
 *
 * <p>El hash de la contrasena nunca sale de aqui: no aparece en DTO, ni en
 * respuestas del API, ni en bitacora.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(name = "s_usuario", nullable = false, length = 80)
    private String usuario;

    /** Hash BCrypt. Jamas texto plano. */
    @Column(name = "s_password_hash", nullable = false, length = 100)
    private String passwordHash;

    @NotBlank
    @Column(name = "s_nombre", nullable = false, length = 120)
    private String nombre;

    @NotBlank
    @Column(name = "s_apellido_paterno", nullable = false, length = 120)
    private String apellidoPaterno;

    @Column(name = "s_apellido_materno", length = 120)
    private String apellidoMaterno;

    @Email
    @NotBlank
    @Column(name = "s_correo", nullable = false, length = 150)
    private String correo;

    @Column(name = "s_telefono", length = 30)
    private String telefono;

    /** Adscripcion; sirve para filtrar la bandeja de trabajo por sede. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_sede_id",
                foreignKey = @ForeignKey(name = "fk_usuario_cat_sede"))
    private Sede sede;

    /** Borrado logico: un usuario dado de baja conserva su historial. */
    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /**
     * Obliga a cambiar la contrasena en el siguiente acceso. El usuario inicial
     * se crea con esto en verdadero: no existe contrasena por defecto conocida.
     */
    @Column(name = "b_cambio_password_requerido", nullable = false)
    private Boolean cambioPasswordRequerido = Boolean.TRUE;

    @Column(name = "d_ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    @Column(name = "n_intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /** Fin del bloqueo temporal por intentos fallidos; nulo si no esta bloqueado. */
    @Column(name = "d_bloqueo_hasta")
    private OffsetDateTime bloqueoHasta;

    /**
     * Versión de los tokens de la cuenta. Viaja en cada token; subirla invalida de inmediato todas
     * las sesiones abiertas (cambio o reinicio de contraseña, baja de la cuenta).
     */
    @Column(name = "n_token_version", nullable = false)
    private Integer tokenVersion = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "fn_usuario_id",
                    foreignKey = @ForeignKey(name = "fk_usuario_rol_usuario")),
            inverseJoinColumns = @JoinColumn(name = "fn_rol_id",
                    foreignKey = @ForeignKey(name = "fk_usuario_rol_cat_rol")))
    private Set<Rol> roles = new HashSet<>();

    /** Nombre completo para mostrar en pantalla y en bitacora. */
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder(nombre).append(' ').append(apellidoPaterno);
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            sb.append(' ').append(apellidoMaterno);
        }
        return sb.toString();
    }

    /** Verdadero mientras siga vigente un bloqueo por intentos fallidos. */
    public boolean estaBloqueado() {
        return bloqueoHasta != null && bloqueoHasta.isAfter(OffsetDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getCambioPasswordRequerido() {
        return cambioPasswordRequerido;
    }

    public void setCambioPasswordRequerido(Boolean cambioPasswordRequerido) {
        this.cambioPasswordRequerido = cambioPasswordRequerido;
    }

    public OffsetDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(OffsetDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public Integer getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(Integer intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public OffsetDateTime getBloqueoHasta() {
        return bloqueoHasta;
    }

    public void setBloqueoHasta(OffsetDateTime bloqueoHasta) {
        this.bloqueoHasta = bloqueoHasta;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    /** Invalida todos los tokens emitidos hasta ahora para esta cuenta. */
    public void incrementarTokenVersion() {
        this.tokenVersion = (tokenVersion == null ? 0 : tokenVersion) + 1;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }
}
