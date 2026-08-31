package mx.gob.sedif.sea.core.folio;

import java.time.OffsetDateTime;

import mx.gob.sedif.sea.core.catalogo.Municipio;

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
 * Contador de folios por tipo, municipio y ano.
 *
 * <p>El servicio toma este renglon con bloqueo antes de incrementarlo, de modo
 * que dos altas simultaneas no puedan obtener el mismo numero. Sin ese bloqueo,
 * el dia de una jornada con varias personas capturando a la vez saldrian folios
 * repetidos.
 */
@Entity
@Table(name = "folio_consecutivo")
public class FolioConsecutivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    /** SOLICITUD, CERTIFICADO, CITA o ANIMAL. */
    @Column(name = "s_tipo", nullable = false, length = 30)
    private String tipo;

    /** Nulo en los consecutivos que no dependen del municipio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_municipio_id",
                foreignKey = @ForeignKey(name = "fk_folio_consecutivo_cat_municipio"))
    private Municipio municipio;

    @Column(name = "n_anio", nullable = false)
    private Integer anio;

    @Column(name = "n_consecutivo", nullable = false)
    private Long consecutivo = 0L;

    @Column(name = "d_fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();

    @Column(name = "d_fecha_modificacion")
    private OffsetDateTime fechaModificacion;

    /** Avanza el contador y devuelve el numero asignado. */
    public long siguiente() {
        this.consecutivo = this.consecutivo + 1;
        this.fechaModificacion = OffsetDateTime.now();
        return this.consecutivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Long getConsecutivo() {
        return consecutivo;
    }

    public void setConsecutivo(Long consecutivo) {
        this.consecutivo = consecutivo;
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
