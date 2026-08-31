package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Especie registrable: canino o felino.
 *
 * <p>Un solo nombre por especie. En el legado convivian PERRO y CANINO como
 * especies distintas porque el campo era texto libre.
 */
@Entity
@Table(name = "cat_especie")
public class Especie extends CatalogoBase {
}
