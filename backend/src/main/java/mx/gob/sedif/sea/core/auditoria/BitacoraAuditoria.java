package mx.gob.sedif.sea.core.auditoria;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import jakarta.validation.constraints.NotBlank;

/**
 * Registro de las acciones sensibles del sistema.
 *
 * <p>Deja constancia, como minimo, de aprobaciones, rechazos, descargas de
 * documentos personales, consultas por CURP, cambios en catalogos y altas o
 * bajas de usuarios.
 *
 * <p>El legado no registraba nada: no habia forma de saber quien aprobo un
 * tramite ni quien consulto los datos de una persona.
 *
 * <p>Desde V20 también guarda cada petición al API (filtro de actividad) y es de solo inserción:
 * un disparador en la base rechaza UPDATE, DELETE y TRUNCATE, y la entidad es inmutable para
 * Hibernate. Área: Núcleo compartido (auditoría).
 */
@Entity
@Immutable
@Table(name = "bitacora_auditoria")
public class BitacoraAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    /** Nulo cuando la accion viene del portal publico, sin sesion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id",
                foreignKey = @ForeignKey(name = "fk_bitacora_auditoria_usuario"))
    private Usuario usuario;

    /** Nombre de la entidad afectada: solicitud, persona, documento. */
    @NotBlank
    @Column(name = "s_entidad", nullable = false, length = 80)
    private String entidad;

    /**
     * Identificador del registro afectado.
     *
     * <p>No es llave foranea porque apunta a cualquier tabla del sistema; por
     * eso la columna lleva prefijo de numero y no de referencia.
     */
    @Column(name = "n_registro_id")
    private Long registroId;

    /** APROBAR, RECHAZAR, CONSULTAR_CURP, DESCARGAR_DOCUMENTO, LOGIN... */
    @NotBlank
    @Column(name = "s_accion", nullable = false, length = 60)
    private String accion;

    /**
     * Estado antes y despues del cambio, en JSON.
     *
     * <p>Permite reconstruir que se modifico exactamente sin tener que crear
     * una tabla espejo por cada entidad del sistema.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "j_valor_anterior", columnDefinition = "jsonb")
    private String valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "j_valor_nuevo", columnDefinition = "jsonb")
    private String valorNuevo;

    // PostgreSQL rechaza recibir texto plano en una columna inet: hay que
    // indicarle explicitamente el tipo.
    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "s_ip", columnDefinition = "inet")
    private String ip;

    @Column(name = "s_user_agent", length = 400)
    private String userAgent;

    /** Descripcion legible, para la pantalla de consulta de bitacora. */
    @Column(name = "s_detalle", length = 1000)
    private String detalle;

    @Column(name = "d_fecha", nullable = false, updatable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

    /** Método HTTP de la petición registrada por el filtro de actividad; nulo en eventos explícitos. */
    @Column(name = "s_metodo", length = 10)
    private String metodo;

    /** Ruta del API sin parámetros de consulta (nunca se guardan los valores buscados). */
    @Column(name = "s_ruta", length = 300)
    private String ruta;

    @Column(name = "n_codigo_http")
    private Short codigoHttp;

    /** EXITO, RECHAZADO, DENEGADO o ERROR; nulo en eventos explícitos. */
    @Column(name = "s_resultado", length = 20)
    private String resultado;

    @Column(name = "n_duracion_ms")
    private Integer duracionMs;

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public Short getCodigoHttp() {
        return codigoHttp;
    }

    public void setCodigoHttp(Short codigoHttp) {
        this.codigoHttp = codigoHttp;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public Integer getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(Integer duracionMs) {
        this.duracionMs = duracionMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public void setRegistroId(Long registroId) {
        this.registroId = registroId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(String valorNuevo) {
        this.valorNuevo = valorNuevo;
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

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public void setFecha(OffsetDateTime fecha) {
        this.fecha = fecha;
    }
}
