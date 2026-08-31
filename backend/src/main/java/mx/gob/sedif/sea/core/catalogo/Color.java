package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Colores del pelaje registrables.
 *
 * <p>Sustituye el texto libre del sistema legado, donde este dato se
 * capturaba a mano y generaba variantes del mismo concepto.
 */
@Entity
@Table(name = "cat_color")
public class Color extends CatalogoBase {{
}}
