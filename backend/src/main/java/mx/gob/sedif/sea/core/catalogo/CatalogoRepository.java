package mx.gob.sedif.sea.core.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * Consultas comunes a todos los catalogos.
 *
 * <p>No es un repositorio en si: cada catalogo declara el suyo extendiendo esta
 * interfaz, y Spring Data genera la implementacion. Asi las trece consultas
 * equivalentes se escriben una sola vez.
 *
 * <p>Las consultas usan parametros con enlace, nunca concatenacion de cadenas.
 *
 * @param <T> entidad de catalogo
 */
@NoRepositoryBean
public interface CatalogoRepository<T extends CatalogoBase> extends JpaRepository<T, Integer> {

    /** Busca por la clave estable que usa el codigo. */
    Optional<T> findByClave(String clave);

    boolean existsByClave(String clave);

    /** Comprueba duplicados al editar, sin contarse a si mismo. */
    boolean existsByClaveAndIdNot(String clave, Integer id);

    /** Los vigentes, en el orden definido para desplegar. */
    List<T> findByActivoTrueOrderByOrdenAscNombreAsc();

    /**
     * Listado paginado para el panel, con filtro de texto opcional sobre clave
     * y nombre. Ignora acentos y mayusculas: buscar "tehuacan" encuentra
     * "Tehuacán".
     *
     * <p>El filtro se recibe ya envuelto en comodines y en mayusculas, con
     * cadena vacia en lugar de nulo. Es a proposito: PostgreSQL no puede
     * deducir el tipo de un parametro nulo dentro de UPPER() y rechaza la
     * consulta, asi que se evita pasarle nulos.
     */
    @Query("""
            SELECT c FROM #{#entityName} c
            WHERE (:soloActivos = FALSE OR c.activo = TRUE)
              AND (:patron = ''
                   OR sea_unaccent(UPPER(c.nombre)) LIKE sea_unaccent(:patron)
                   OR UPPER(c.clave) LIKE :patron)
            """)
    Page<T> buscarPorPatron(@Param("patron") String patron,
                            @Param("soloActivos") boolean soloActivos,
                            Pageable pageable);

    /**
     * Listado paginado con filtro de texto opcional.
     *
     * @param texto texto a buscar; nulo o vacio para no filtrar
     */
    default Page<T> buscar(String texto, boolean soloActivos, Pageable pageable) {
        String patron = (texto == null || texto.isBlank())
                ? ""
                : "%" + texto.trim().toUpperCase() + "%";
        return buscarPorPatron(patron, soloActivos, pageable);
    }
}
