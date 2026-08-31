package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Via por la que se le avisa a la persona: correo, telefono o WhatsApp.
 */
@Entity
@Table(name = "cat_medio_contacto")
public class MedioContacto extends CatalogoBase {

    /**
     * Verdadero solo para el correo, que el sistema envia solo.
     *
     * <p>Cuando es falso el aviso lo tiene que dar una persona, y la
     * notificacion cae en la bandeja de contacto manual. Sin modelar esto, esos
     * avisos simplemente no ocurren y nadie se entera de que hay gente
     * esperando respuesta.
     */
    @Column(name = "b_automatizable", nullable = false)
    private Boolean automatizable = Boolean.FALSE;

    public Boolean getAutomatizable() {
        return automatizable;
    }

    public void setAutomatizable(Boolean automatizable) {
        this.automatizable = automatizable;
    }
}
