package mx.gob.sedif.sea.security.sesion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a las sesiones del personal.
 * Área: Núcleo compartido (seguridad). Lógica: rotación y revocación del token de refresco.
 */
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    /**
     * Sesión por su identificador público, con su usuario cargado.
     *
     * @param identificador claim {@code sid} del token de refresco
     * @return la sesión, abierta o no; vacío si no existe
     */
    @EntityGraph(attributePaths = "usuario")
    Optional<SesionUsuario> findByIdentificador(UUID identificador);

    /**
     * Verdadero si la sesión sigue abierta y sin vencer. Lo consulta el filtro en cada petición
     * para que cerrar sesión invalide al instante el token de acceso.
     *
     * @param identificador claim {@code sid} del token
     * @param ahora         momento de la comprobación
     * @return verdadero si la sesión existe, no está revocada y no ha vencido
     */
    boolean existsByIdentificadorAndRevocadaIsNullAndExpiraAfter(UUID identificador,
                                                                 OffsetDateTime ahora);

    /**
     * Sesiones todavía abiertas de una cuenta, para revocarlas todas de una vez.
     *
     * @param usuarioId identificador de la cuenta
     * @return las sesiones sin fecha de revocación; lista vacía si no hay
     */
    @Query("SELECT s FROM SesionUsuario s WHERE s.usuario.id = :usuarioId AND s.revocada IS NULL")
    List<SesionUsuario> abiertasDe(@Param("usuarioId") Long usuarioId);

    /**
     * Borra las sesiones que vencieron o se cerraron antes del límite indicado.
     * La bitácora conserva el registro de cada inicio y cierre; esta tabla solo sirve para validar
     * tokens vigentes.
     *
     * @param limite se borran las sesiones vencidas antes de esta fecha
     * @return número de filas borradas
     */
    @Modifying
    @Query("DELETE FROM SesionUsuario s WHERE s.expira < :limite")
    int borrarVencidasAntesDe(@Param("limite") OffsetDateTime limite);
}
