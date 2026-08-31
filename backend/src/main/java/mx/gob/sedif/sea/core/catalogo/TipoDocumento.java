package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Tipo de documento que se puede adjuntar a una solicitud, con sus reglas de
 * validacion: si es obligatorio, cuanto puede pesar y en que formatos.
 *
 * <p>Las reglas viven aqui y no en el codigo para que el personal pueda
 * ajustarlas sin recompilar.
 */
@Entity
@Table(name = "cat_tipo_documento")
public class TipoDocumento extends CatalogoBase {

    @Column(name = "b_requerido_persona", nullable = false)
    private Boolean requeridoPersona = Boolean.FALSE;

    @Column(name = "b_requerido_animal", nullable = false)
    private Boolean requeridoAnimal = Boolean.FALSE;

    @Min(1)
    @Max(50)
    @Column(name = "n_tamano_maximo_mb", nullable = false)
    private Integer tamanoMaximoMb = 5;

    /**
     * Extensiones aceptadas, separadas por comas. Es un filtro de conveniencia
     * para la interfaz: la validacion real comprueba el tipo MIME por contenido
     * del archivo, porque renombrar un ejecutable a .jpg no debe bastar.
     */
    @Column(name = "s_extensiones_permitidas", nullable = false, length = 200)
    private String extensionesPermitidas = "pdf,jpg,jpeg,png";

    public Boolean getRequeridoPersona() {
        return requeridoPersona;
    }

    public void setRequeridoPersona(Boolean requeridoPersona) {
        this.requeridoPersona = requeridoPersona;
    }

    public Boolean getRequeridoAnimal() {
        return requeridoAnimal;
    }

    public void setRequeridoAnimal(Boolean requeridoAnimal) {
        this.requeridoAnimal = requeridoAnimal;
    }

    public Integer getTamanoMaximoMb() {
        return tamanoMaximoMb;
    }

    public void setTamanoMaximoMb(Integer tamanoMaximoMb) {
        this.tamanoMaximoMb = tamanoMaximoMb;
    }

    public String getExtensionesPermitidas() {
        return extensionesPermitidas;
    }

    public void setExtensionesPermitidas(String extensionesPermitidas) {
        this.extensionesPermitidas = extensionesPermitidas;
    }
}
