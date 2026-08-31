package mx.gob.sedif.sea.core.notificacion;

import mx.gob.sedif.sea.core.catalogo.MedioContacto;
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
 * Texto base de un aviso, editable desde el panel.
 *
 * <p>Los textos no viven en el codigo a proposito: la institucion pasa de
 * SMADSOT a DIF y las leyendas van a cambiar. Cambiar una redaccion no deberia
 * exigir recompilar y desplegar.
 *
 * <p>El cuerpo admite marcadores como <code>{{folio}}</code> que se sustituyen
 * al momento de enviar.
 */
@Entity
@Table(name = "plantilla_notificacion")
public class PlantillaNotificacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @NotBlank
    @Column(name = "s_clave", nullable = false, length = 60, updatable = false)
    private String clave;

    @NotBlank
    @Column(name = "s_nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "s_descripcion", length = 500)
    private String descripcion;

    @NotBlank
    @Column(name = "s_asunto", nullable = false, length = 300)
    private String asunto;

    @NotBlank
    @Column(name = "s_cuerpo", nullable = false, columnDefinition = "text")
    private String cuerpo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_medio_contacto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_plantilla_notificacion_medio"))
    private MedioContacto medioContacto;

    /** Marcadores que acepta la plantilla, para mostrarlos como ayuda al editar. */
    @Column(name = "s_variables_disponibles", length = 500)
    private String variablesDisponibles;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
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

    public MedioContacto getMedioContacto() {
        return medioContacto;
    }

    public void setMedioContacto(MedioContacto medioContacto) {
        this.medioContacto = medioContacto;
    }

    public String getVariablesDisponibles() {
        return variablesDisponibles;
    }

    public void setVariablesDisponibles(String variablesDisponibles) {
        this.variablesDisponibles = variablesDisponibles;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
