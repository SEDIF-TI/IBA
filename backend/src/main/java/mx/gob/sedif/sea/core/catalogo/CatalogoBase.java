package mx.gob.sedif.sea.core.catalogo;

import mx.gob.sedif.sea.util.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Forma comun de los catalogos administrables.
 *
 * <p>Todos comparten la misma estructura, asi que se define una vez: identificador,
 * clave estable, nombre, descripcion, orden de despliegue, bandera de borrado
 * logico y auditoria.
 *
 * <p>La distincion importante es entre {@code clave} y {@code nombre}:
 * <ul>
 *   <li>{@code clave} es la referencia estable que usa el codigo
 *       (ej. {@code "CANINO"}). No cambia nunca.</li>
 *   <li>{@code nombre} es lo que ve y edita el personal desde el panel
 *       (ej. {@code "Canino"}). Puede cambiar sin romper nada.</li>
 * </ul>
 *
 * <p>El borrado es logico: un catalogo referenciado por registros historicos
 * no puede desaparecer sin dejar huerfanos.
 */
@MappedSuperclass
public abstract class CatalogoBase extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "s_clave", nullable = false, length = 50, updatable = false)
    private String clave;

    @NotBlank
    @Size(max = 150)
    @Column(name = "s_nombre", nullable = false, length = 150)
    private String nombre;

    @Size(max = 500)
    @Column(name = "s_descripcion", length = 500)
    private String descripcion;

    @Column(name = "n_orden", nullable = false)
    private Integer orden = 0;

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

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
