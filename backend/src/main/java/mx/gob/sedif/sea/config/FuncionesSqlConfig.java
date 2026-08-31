package mx.gob.sedif.sea.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Declara ante Hibernate las funciones SQL propias del proyecto.
 *
 * <p>Sin esto, {@code FUNCTION('sea_unaccent', ...)} en una consulta HQL se
 * interpreta con tipo de retorno {@code Object}, y Hibernate 6 rechaza usarlo
 * como operando de {@code LIKE} porque no lo reconoce como texto.
 *
 * <p>Registrarla ademas permite escribirla como funcion normal en HQL
 * ({@code sea_unaccent(x)}) en lugar de la forma generica.
 *
 * <p>La funcion la crea la migracion V1: es un envoltorio IMMUTABLE de
 * {@code unaccent()}, necesario para poder indexar busquedas sin acentos.
 */
public class FuncionesSqlConfig implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions contribuciones) {
        contribuciones.getFunctionRegistry()
                .registerPattern("sea_unaccent",
                        "sea_unaccent(?1)",
                        contribuciones.getTypeConfiguration()
                                .getBasicTypeRegistry()
                                .resolve(StandardBasicTypes.STRING));
    }
}
