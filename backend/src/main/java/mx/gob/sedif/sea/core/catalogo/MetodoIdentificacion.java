package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Forma en que el animal esta identificado: microchip, placa, tatuaje o ninguno.
 */
@Entity
@Table(name = "cat_metodo_identificacion")
public class MetodoIdentificacion extends CatalogoBase {

    /**
     * Cuando es verdadero, la captura exige el numero de chip o placa.
     * Para "ninguno" es falso.
     */
    @Column(name = "b_requiere_identificador", nullable = false)
    private Boolean requiereIdentificador = Boolean.FALSE;

    public Boolean getRequiereIdentificador() {
        return requiereIdentificador;
    }

    public void setRequiereIdentificador(Boolean requiereIdentificador) {
        this.requiereIdentificador = requiereIdentificador;
    }
}
