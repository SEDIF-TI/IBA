package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Raza, siempre subordinada a una especie: un gato no puede ser Chihuahua.
 *
 * <p>Incluye "Mestizo / Criollo" en ambas especies, que es la mas frecuente
 * del padron.
 */
@Entity
@Table(name = "cat_raza")
public class Raza extends CatalogoBase {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_especie_id", nullable = false,
                foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cat_raza_cat_especie"))
    private Especie especie;

    public Especie getEspecie() {
        return especie;
    }

    public void setEspecie(Especie especie) {
        this.especie = especie;
    }
}
