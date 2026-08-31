package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Talla aproximada: chico, mediano o grande.
 *
 * <p>Sustituye el texto libre del sistema legado, donde este dato se
 * capturaba a mano y generaba variantes del mismo concepto.
 */
@Entity
@Table(name = "cat_tamano")
public class Tamano extends CatalogoBase {{
}}
