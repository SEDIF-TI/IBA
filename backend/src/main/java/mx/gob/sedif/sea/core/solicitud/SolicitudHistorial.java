package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;

import mx.gob.sedif.sea.core.catalogo.EstatusSolicitud;
import mx.gob.sedif.sea.core.catalogo.MotivoRechazo;
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
import jakarta.validation.constraints.NotNull;

/**
 * Un renglon por cada cambio de estado del tramite.
 *
 * <p>Toda transicion escribe aqui, sin excepcion: es la memoria del expediente.
 * El legado no registraba nada, asi que no habia forma de saber quien aprobo un
 * tramite ni por que se rechazo.
 */
@Entity
@Table(name = "solicitud_historial")
public class SolicitudHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_solicitud_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_historial_solicitud"))
    private Solicitud solicitud;

    /** Nulo solo en el primer renglon: el alta no viene de ningun estado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_estatus_anterior_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_historial_estatus_anterior"))
    private EstatusSolicitud estatusAnterior;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_estatus_nuevo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_solicitud_historial_estatus_nuevo"))
    private EstatusSolicitud estatusNuevo;

    /** Nulo cuando el movimiento lo produce el portal publico, sin sesion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_historial_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_motivo_rechazo_id",
                foreignKey = @ForeignKey(name = "fk_solicitud_historial_motivo"))
    private MotivoRechazo motivoRechazo;

    @Column(name = "s_observacion", length = 1000)
    private String observacion;

    @Column(name = "d_fecha", nullable = false, updatable = false)
    private OffsetDateTime fecha = OffsetDateTime.now();

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

    public EstatusSolicitud getEstatusAnterior() {
        return estatusAnterior;
    }

    public void setEstatusAnterior(EstatusSolicitud estatusAnterior) {
        this.estatusAnterior = estatusAnterior;
    }

    public EstatusSolicitud getEstatusNuevo() {
        return estatusNuevo;
    }

    public void setEstatusNuevo(EstatusSolicitud estatusNuevo) {
        this.estatusNuevo = estatusNuevo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public MotivoRechazo getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(MotivoRechazo motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public void setFecha(OffsetDateTime fecha) {
        this.fecha = fecha;
    }
}
