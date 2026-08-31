package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;
import java.util.UUID;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.catalogo.EstatusSolicitud;
import mx.gob.sedif.sea.core.catalogo.MotivoRechazo;
import mx.gob.sedif.sea.core.jornada.Jornada;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.util.Auditable;
import mx.gob.sedif.sea.util.enums.CanalSolicitud;

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
import jakarta.validation.constraints.NotNull;

/**
 * Tramite de empadronamiento: una persona y un animal.
 *
 * <p>El vinculo entre ambos NO se lee de aqui, sino de {@code titularidad},
 * que lleva vigencia. Esta entidad solo dice de que animal trata este tramite
 * concreto.
 */
@Entity
@Table(name = "solicitud")
public class Solicitud extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    /**
     * Folio visible para la ciudadania, con patron configurable.
     *
     * <p>Se conserva cuando una solicitud observada se corrige: es el mismo
     * tramite y la persona ya lo tiene anotado.
     */
    @Column(name = "s_folio_solicitud", nullable = false, length = 60, updatable = false)
    private String folioSolicitud;

    /**
     * Token opaco para el seguimiento publico y la liga de correccion.
     * No se puede deducir a partir del folio.
     */
    @Column(name = "s_token_consulta", nullable = false, updatable = false)
    private UUID tokenConsulta;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_persona_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_persona"))
    private Persona persona;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_animal_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_animal"))
    private Animal animal;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_estatus_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_cat_estatus"))
    private EstatusSolicitud estatus;

    /** Solo las precargas de una jornada la llevan. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_jornada_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_jornada"))
    private Jornada jornada;

    /** Por donde entro el tramite. Se persiste como texto, no como ordinal. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "s_canal", nullable = false, length = 20)
    private CanalSolicitud canal;

    @Column(name = "d_fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();

    @Column(name = "d_fecha_resolucion")
    private OffsetDateTime fechaResolucion;

    /** Quien resolvio. Junto con la fecha, permite rendir cuentas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_resolucion_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_usuario_resolucion"))
    private Usuario usuarioResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_motivo_rechazo_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_cat_motivo_rechazo"))
    private MotivoRechazo motivoRechazo;

    @Column(name = "s_observacion_resolucion", length = 1000)
    private String observacionResolucion;

    /**
     * La marcan las reglas anti suplantacion: la CURP no cuadra con el nombre,
     * o los datos de contacto difieren de los que ya tenia esa persona.
     * Como no hay autenticacion ciudadana, es el unico control disponible.
     */
    @Column(name = "b_revision_reforzada", nullable = false)
    private Boolean revisionReforzada = Boolean.FALSE;

    @Column(name = "s_motivo_revision_reforzada", length = 500)
    private String motivoRevisionReforzada;

    /**
     * Evidencia de consentimiento. Sin cuenta de usuario, es la unica
     * constancia de que la persona acepto el tratamiento de sus datos. Por eso
     * los tres campos son obligatorios.
     */
    @Column(name = "s_aviso_privacidad_version", nullable = false, length = 20, updatable = false)
    private String avisoPrivacidadVersion;

    @Column(name = "d_fecha_aceptacion_aviso", nullable = false, updatable = false)
    private OffsetDateTime fechaAceptacionAviso;

    /** Se mapea como texto; en la tabla la columna es de tipo INET. */
    @Column(name = "s_ip_origen", nullable = false, columnDefinition = "inet", updatable = false)
    private String ipOrigen;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /** Marca la solicitud para revision reforzada, dejando dicho por que. */
    public void marcarRevisionReforzada(String motivo) {
        this.revisionReforzada = Boolean.TRUE;
        this.motivoRevisionReforzada = motivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFolioSolicitud() {
        return folioSolicitud;
    }

    public void setFolioSolicitud(String folioSolicitud) {
        this.folioSolicitud = folioSolicitud;
    }

    public UUID getTokenConsulta() {
        return tokenConsulta;
    }

    public void setTokenConsulta(UUID tokenConsulta) {
        this.tokenConsulta = tokenConsulta;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public EstatusSolicitud getEstatus() {
        return estatus;
    }

    public void setEstatus(EstatusSolicitud estatus) {
        this.estatus = estatus;
    }

    public Jornada getJornada() {
        return jornada;
    }

    public void setJornada(Jornada jornada) {
        this.jornada = jornada;
    }

    public CanalSolicitud getCanal() {
        return canal;
    }

    public void setCanal(CanalSolicitud canal) {
        this.canal = canal;
    }

    public OffsetDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(OffsetDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public OffsetDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(OffsetDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public Usuario getUsuarioResolucion() {
        return usuarioResolucion;
    }

    public void setUsuarioResolucion(Usuario usuarioResolucion) {
        this.usuarioResolucion = usuarioResolucion;
    }

    public MotivoRechazo getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(MotivoRechazo motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getObservacionResolucion() {
        return observacionResolucion;
    }

    public void setObservacionResolucion(String observacionResolucion) {
        this.observacionResolucion = observacionResolucion;
    }

    public Boolean getRevisionReforzada() {
        return revisionReforzada;
    }

    public void setRevisionReforzada(Boolean revisionReforzada) {
        this.revisionReforzada = revisionReforzada;
    }

    public String getMotivoRevisionReforzada() {
        return motivoRevisionReforzada;
    }

    public void setMotivoRevisionReforzada(String motivoRevisionReforzada) {
        this.motivoRevisionReforzada = motivoRevisionReforzada;
    }

    public String getAvisoPrivacidadVersion() {
        return avisoPrivacidadVersion;
    }

    public void setAvisoPrivacidadVersion(String avisoPrivacidadVersion) {
        this.avisoPrivacidadVersion = avisoPrivacidadVersion;
    }

    public OffsetDateTime getFechaAceptacionAviso() {
        return fechaAceptacionAviso;
    }

    public void setFechaAceptacionAviso(OffsetDateTime fechaAceptacionAviso) {
        this.fechaAceptacionAviso = fechaAceptacionAviso;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
