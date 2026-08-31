package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import mx.gob.sedif.sea.core.catalogo.Municipio;
import mx.gob.sedif.sea.security.Usuario;
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
import jakarta.validation.constraints.Positive;

/**
 * Jornada municipal de empadronamiento.
 *
 * <p>El personal viaja al municipio. La jornada se publica por anticipado, la
 * ciudadania precarga sus registros, y el dia del evento se busca por CURP o
 * apellidos, se valida en sitio y se imprime en el momento.
 */
@Entity
@Table(name = "jornada")
public class Jornada extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotBlank
    @Column(name = "s_nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "s_descripcion", length = 1000)
    private String descripcion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_municipio_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_jornada_cat_municipio"))
    private Municipio municipio;

    @NotBlank
    @Column(name = "s_domicilio_sede", nullable = false, length = 400)
    private String domicilioSede;

    @NotNull
    @Column(name = "d_fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(name = "d_fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "s_horario", length = 120)
    private String horario;

    /** Nulo cuando la jornada no tiene limite de atencion. */
    @Positive
    @Column(name = "n_cupo")
    private Integer cupo;

    /** Solo las publicadas aparecen en el portal publico. */
    @Column(name = "b_publicada", nullable = false)
    private Boolean publicada = Boolean.FALSE;

    @Column(name = "d_fecha_publicacion")
    private OffsetDateTime fechaPublicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_responsable_id",
                foreignKey = @ForeignKey(name = "fk_jornada_usuario_responsable"))
    private Usuario usuarioResponsable;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /** Publica la jornada, dejando constancia de cuando ocurrio. */
    public void publicar() {
        this.publicada = Boolean.TRUE;
        this.fechaPublicacion = OffsetDateTime.now();
    }

    /** Verdadero si la jornada se esta celebrando hoy. */
    public boolean estaEnCurso() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public String getDomicilioSede() {
        return domicilioSede;
    }

    public void setDomicilioSede(String domicilioSede) {
        this.domicilioSede = domicilioSede;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Integer getCupo() {
        return cupo;
    }

    public void setCupo(Integer cupo) {
        this.cupo = cupo;
    }

    public Boolean getPublicada() {
        return publicada;
    }

    public void setPublicada(Boolean publicada) {
        this.publicada = publicada;
    }

    public OffsetDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(OffsetDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public Usuario getUsuarioResponsable() {
        return usuarioResponsable;
    }

    public void setUsuarioResponsable(Usuario usuarioResponsable) {
        this.usuarioResponsable = usuarioResponsable;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
