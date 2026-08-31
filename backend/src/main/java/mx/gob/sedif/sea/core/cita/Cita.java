package mx.gob.sedif.sea.core.cita;

import java.time.LocalDate;

import mx.gob.sedif.sea.core.catalogo.EstatusCita;
import mx.gob.sedif.sea.core.catalogo.Sede;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.util.Auditable;

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
import jakarta.validation.constraints.NotNull;

/**
 * Cita para recoger la documentacion en una sede.
 */
@Entity
@Table(name = "cita")
public class Cita extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_solicitud_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cita_solicitud"))
    private Solicitud solicitud;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_sede_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cita_cat_sede"))
    private Sede sede;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_estatus_cita_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cita_cat_estatus_cita"))
    private EstatusCita estatus;

    @NotBlank
    @Column(name = "s_numero_cita", nullable = false, length = 40, updatable = false)
    private String numeroCita;

    @NotNull
    @Column(name = "d_fecha_cita", nullable = false)
    private LocalDate fechaCita;

    @Column(name = "s_horario", length = 60)
    private String horario;

    /** Apunta a la cita previa cuando esta es una reprogramacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_cita_anterior_id",
                foreignKey = @ForeignKey(name = "fk_cita_cita_anterior"))
    private Cita citaAnterior;

    @Column(name = "s_observacion", length = 500)
    private String observacion;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

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

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public EstatusCita getEstatus() {
        return estatus;
    }

    public void setEstatus(EstatusCita estatus) {
        this.estatus = estatus;
    }

    public String getNumeroCita() {
        return numeroCita;
    }

    public void setNumeroCita(String numeroCita) {
        this.numeroCita = numeroCita;
    }

    public LocalDate getFechaCita() {
        return fechaCita;
    }

    public void setFechaCita(LocalDate fechaCita) {
        this.fechaCita = fechaCita;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Cita getCitaAnterior() {
        return citaAnterior;
    }

    public void setCitaAnterior(Cita citaAnterior) {
        this.citaAnterior = citaAnterior;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
