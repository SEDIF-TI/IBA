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
     */
    @Query("""
            SELECT c FROM #{#entityName} c
            WHERE (:soloActivos = FALSE OR c.activo = TRUE)
              AND (:texto IS NULL
                   OR sea_unaccent(UPPER(c.nombre)) LIKE sea_unaccent(UPPER(CONCAT('%', :texto, '%')))
                   OR UPPER(c.clave) LIKE UPPER(CONCAT('%', :texto, '%')))
            """)
    Page<T> buscar(@Param("texto") String texto,
                   @Param("soloActivos") boolean soloActivos,
                   Pageable pageable);
}
