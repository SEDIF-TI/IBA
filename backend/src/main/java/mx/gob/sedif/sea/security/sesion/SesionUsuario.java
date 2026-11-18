package mx.gob.sedif.sea.security.sesion;

import java.time.OffsetDateTime;
import java.util.UUID;

import mx.gob.sedif.sea.security.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Sesión abierta del personal en un dispositivo (tabla {@code sesion_usuario}).
 * Área: Núcleo compartido (seguridad). Lógica: emisión, rotación y revocación del token de refresco.
 *
 * <p>Cada inicio de sesión crea una fila. El token de refresco lleva el {@link #identificador} de su
 * sesión y un identificador de un solo uso, del que aquí solo se guarda la huella SHA-256. Al
 * renovar, la huella cambia; si llega un token con una huella anterior, alguien lo copió y la
 * sesión se revoca.
 */
@Entity
@Table(name = "sesion_usuario")
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    /** Identificador público de la sesión; claim {@code sid} del token de refresco. */
    @Column(name = "s_identificador", nullable = false, unique = true)
    private UUID identificador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sesion_usuario_usuario"))
    private Usuario usuario;

    /** SHA-256 en hexadecimal del identificador de un solo uso vigente. */
    @Column(name = "s_huella_refresco", nullable = false, length = 64)
    private String huellaRefresco;

    @Column(name = "d_fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "d_ultimo_uso")
    private OffsetDateTime ultimoUso;

    @Column(name = "d_expira", nullable = false)
    private OffsetDateTime expira;

    /** Momento en que se cerró la sesión; nulo mientras siga abierta. */
    @Column(name = "d_revocada")
    private OffsetDateTime revocada;

    /** Motivo del cierre: cierre de sesión, cambio de contraseña, reutilización del token, etc. */
    @Column(name = "s_motivo_revocacion", length = 40)
    private String motivoRevocacion;

    @Column(name = "s_ip", length = 45)
    private String ip;

    @Column(name = "s_user_agent", length = 400)
    private String userAgent;

    /** Verdadero si la sesión no se ha cerrado y no ha vencido. */
    public boolean estaAbierta() {
        return revocada == null && expira.isAfter(OffsetDateTime.now());
    }

    /**
     * Cierra la sesión. No hace nada si ya estaba cerrada, para conservar el primer motivo.
     *
     * @param motivo causa del cierre, como máximo 40 caracteres
     */
    public void revocar(String motivo) {
        if (revocada == null) {
            revocada = OffsetDateTime.now();
            motivoRevocacion = motivo;
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getIdentificador() {
        return identificador;
    }

    public void setIdentificador(UUID identificador) {
        this.identificador = identificador;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getHuellaRefresco() {
        return huellaRefresco;
    }

    public void setHuellaRefresco(String huellaRefresco) {
        this.huellaRefresco = huellaRefresco;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getUltimoUso() {
        return ultimoUso;
    }

    public void setUltimoUso(OffsetDateTime ultimoUso) {
        this.ultimoUso = ultimoUso;
    }

    public OffsetDateTime getExpira() {
        return expira;
    }

    public void setExpira(OffsetDateTime expira) {
        this.expira = expira;
    }

    public OffsetDateTime getRevocada() {
        return revocada;
    }

    public String getMotivoRevocacion() {
        return motivoRevocacion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
