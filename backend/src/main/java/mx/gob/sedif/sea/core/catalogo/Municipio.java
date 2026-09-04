package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Municipio del estado de Puebla. Son 217, sembrados en la migracion V12.
 */
@Entity
@Table(name = "cat_municipio")
public class Municipio extends CatalogoBase {

    /**
     * Clave geoestadistica del INEGI: entidad (21 = Puebla) mas los tres
     * dígitos del municipio. Se usa para construir el folio oficial del
     * certificado.
     */
    @NotBlank
    @Pattern(regexp = "^[0-9]{5}$", message = "La clave INEGI debe tener 5 dígitos")
    @Column(name = "s_cve_inegi", nullable = false, length = 5)
    private String claveInegi;

    public String getClaveInegi() {
        return claveInegi;
    }

    public void setClaveInegi(String claveInegi) {
        this.claveInegi = claveInegi;
    }
}
