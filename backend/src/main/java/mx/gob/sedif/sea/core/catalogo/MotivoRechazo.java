package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Motivo por el que una solicitud se observa o se rechaza.
 *
 * <p>Es catalogo y no texto libre para poder medir: sin esto no hay forma de
 * saber que el INE ilegible es el motivo mas frecuente, ni de actuar en
 * consecuencia.
 */
@Entity
@Table(name = "cat_motivo_rechazo")
public class MotivoRechazo extends CatalogoBase {

    /**
     * Verdadero cuando la persona puede corregir y reenviar: la solicitud pasa
     * a OBSERVADA y se le manda una liga con token. Falso cuando es causa de
     * rechazo definitivo (duplicado, fuera de jurisdiccion, datos falsos).
     */
    @Column(name = "b_permite_correccion", nullable = false)
    private Boolean permiteCorreccion = Boolean.TRUE;

    public Boolean getPermiteCorreccion() {
        return permiteCorreccion;
    }

    public void setPermiteCorreccion(Boolean permiteCorreccion) {
        this.permiteCorreccion = permiteCorreccion;
    }
}
