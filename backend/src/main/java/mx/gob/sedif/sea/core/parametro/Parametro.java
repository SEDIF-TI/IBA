package mx.gob.sedif.sea.core.parametro;

import mx.gob.sedif.sea.util.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Ajuste configurable del sistema.
 *
 * <p>Aqui viven los patrones de folio, las vigencias, las leyendas de los
 * documentos y la identidad grafica. Nada de eso esta en el codigo: la
 * institucion pasa de SMADSOT a DIF y todo eso cambia sin que deba recompilarse
 * el sistema.
 */
@Entity
@Table(name = "parametro")
public class Parametro extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Integer id;

    @NotBlank
    @Column(name = "s_clave", nullable = false, length = 80, updatable = false)
    private String clave;

    @NotBlank
    @Column(name = "s_valor", nullable = false, columnDefinition = "text")
    private String valor;

    @Column(name = "s_descripcion", length = 500)
    private String descripcion;

    /** TEXTO, ENTERO, DECIMAL, BOOLEANO, FECHA o JSON: valida antes de guardar. */
    @NotBlank
    @Column(name = "s_tipo_dato", nullable = false, length = 20)
    private String tipoDato = "TEXTO";

    /** Agrupa los ajustes por seccion en la pantalla de configuracion. */
    @Column(name = "s_grupo", length = 60)
    private String grupo;

    /** Protegido: el sistema lo necesita, no se borra desde el panel. */
    @Column(name = "b_sistema", nullable = false)
    private Boolean sistema = Boolean.FALSE;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /** Lee el valor como entero. */
    public int comoEntero() {
        return Integer.parseInt(valor.trim());
    }

    /** Lee el valor como booleano. */
    public boolean comoBooleano() {
        return Boolean.parseBoolean(valor.trim());
    }

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

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato = tipoDato;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public Boolean getSistema() {
        return sistema;
    }

    public void setSistema(Boolean sistema) {
        this.sistema = sistema;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
