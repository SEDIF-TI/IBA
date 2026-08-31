package mx.gob.sedif.sea.security;

import mx.gob.sedif.sea.core.catalogo.CatalogoBase;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Permiso granular con formato {@code <modulo>:<accion>}, por ejemplo
 * {@code solicitud:aprobar}.
 *
 * <p>Es lo que evalua {@code @PreAuthorize} en los controladores.
 */
@Entity
@Table(name = "cat_permiso")
// La clave del permiso admite 80 caracteres, no los 50 del resto de catalogos:
// el formato <modulo>:<accion> es mas largo que una clave simple.
@AttributeOverride(name = "clave", column = @Column(name = "s_clave", length = 80, updatable = false))
public class Permiso extends CatalogoBase {

    /** Agrupa los permisos por seccion en la pantalla de administracion de roles. */
    @NotBlank
    @Column(name = "s_modulo", nullable = false, length = 50)
    private String modulo;

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }
}
