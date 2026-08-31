package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Sexo del animal: macho o hembra.
 *
 * <p>Sustituye el texto libre del sistema legado, donde este dato se
 * capturaba a mano y generaba variantes del mismo concepto.
 */
@Entity
@Table(name = "cat_sexo")
public class Sexo extends CatalogoBase {{
}}
