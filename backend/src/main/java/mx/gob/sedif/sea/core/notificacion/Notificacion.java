package mx.gob.sedif.sea.core.notificacion;

import java.time.OffsetDateTime;

import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.util.enums.CanalNotificacion;
import mx.gob.sedif.sea.util.enums.EstatusEnvio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Aviso enviado o por enviar a la persona solicitante.
 *
 * <p>Cuando el canal es MANUAL, este registro es lo que ve el personal en la
 * bandeja de contacto: a quien hay que llamar y por que.
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_solicitud_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notificacion_solicitud"))
    private Solicitud solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_plantilla_id",
                foreignKey = @ForeignKey(name = "fk_notificacion_plantilla"))
    private PlantillaNotificacion plantilla;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "s_canal", nullable = false, length = 20)
    private CanalNotificacion canal;

    /** Correo o numero telefonico, segun el canal. */
    @NotBlank
    @Column(name = "s_destinatario", nullable = false, length = 200)
    private String destinatario;

    @Column(name = "s_asunto", length = 300)
    private String asunto;

    /**
     * Texto ya resuelto, con los marcadores sustituidos.
     *
     * <p>Se guarda para tener constancia exacta de lo que se le comunico a la
     * persona, aunque despues alguien edite la plantilla.
     */
    @Column(name = "s_cuerpo", columnDefinition = "text")
    private String cuerpo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "s_estatus_envio", nullable = false, length = 20)
    private EstatusEnvio estatusEnvio = EstatusEnvio.PENDIENTE;

    @Column(name = "d_fecha_envio")
    private OffsetDateTime fechaEnvio;

    @Column(name = "n_intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "s_error", length = 1000)
    private String error;

    /** Quien hizo la llamada, solo para el canal manual. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_contacto_id",
                foreignKey = @ForeignKey(name = "fk_notificacion_usuario_contacto"))
    private Usuario usuarioContacto;

    @Column(name = "d_fecha_contacto")
    private OffsetDateTime fechaContacto;

    @Column(name = "s_observacion_contacto", length = 1000)
    private String observacionContacto;

    @Column(name = "d_fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "d_fecha_modificacion")
    private OffsetDateTime fechaModificacion;

    /** Deja constancia de un envio automatico exitoso. */
    public void marcarEnviada() {
        this.estatusEnvio = EstatusEnvio.ENVIADA;
        this.fechaEnvio = OffsetDateTime.now();
        this.intentos = this.intentos + 1;
        this.error = null;
    }

    /** Registra un intento fallido junto con la causa. */
    public void marcarFallida(String causa) {
        this.estatusEnvio = EstatusEnvio.FALLIDA;
        this.intentos = this.intentos + 1;
        this.error = causa;
    }

    /**
     * Cierra un contacto manual: quien llamo, cuando y que resulto.
     *
     * @param logrado si se logro hablar con la persona
     */
    public void registrarContactoManual(boolean logrado, String observacion, Usuario responsable) {
        this.estatusEnvio = logrado ? EstatusEnvio.CONTACTADA : EstatusEnvio.NO_CONTACTADA;
        this.observacionContacto = observacion;
        this.usuarioContacto = responsable;
        this.fechaContacto = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public PlantillaNotificacion getPlantilla() {
        return plantilla;
    }

    public void setPlantilla(PlantillaNotificacion plantilla) {
        this.plantilla = plantilla;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public void setCanal(CanalNotificacion canal) {
        this.canal = canal;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public EstatusEnvio getEstatusEnvio() {
        return estatusEnvio;
    }

    public void setEstatusEnvio(EstatusEnvio estatusEnvio) {
        this.estatusEnvio = estatusEnvio;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(OffsetDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Usuario getUsuarioContacto() {
        return usuarioContacto;
    }

    public void setUsuarioContacto(Usuario usuarioContacto) {
        this.usuarioContacto = usuarioContacto;
    }

    public OffsetDateTime getFechaContacto() {
        return fechaContacto;
    }

    public void setFechaContacto(OffsetDateTime fechaContacto) {
        this.fechaContacto = fechaContacto;
    }

    public String getObservacionContacto() {
        return observacionContacto;
    }

    public void setObservacionContacto(String observacionContacto) {
        this.observacionContacto = observacionContacto;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(OffsetDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
