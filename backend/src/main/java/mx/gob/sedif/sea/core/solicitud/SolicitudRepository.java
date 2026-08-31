package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a datos de los tramites. */
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Optional<Solicitud> findByFolioSolicitud(String folio);

    /**
     * Tramite con el tutor, el animal y su situacion ya cargados.
     *
     * <p>El grafo es necesario porque la conversion a DTO ocurre en el
     * controlador, fuera de la transaccion, y ahi una relacion sin cargar ya no
     * se puede resolver.
     */
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal",
                                   "animal.especie", "estatus", "motivoRechazo"})
    Optional<Solicitud> findConDetallePorId(@Param("id") Long id);

    /**
     * Consulta publica de seguimiento: exige folio Y CURP.
     *
     * <p>Nunca solo la CURP. Quien tuviera la CURP de un tercero podria ver sus
     * datos, y no hay autenticacion ciudadana que lo impida.
     */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.folioSolicitud = :folio
              AND s.persona.curp = :curp
              AND s.activo = TRUE
            """)
    Optional<Solicitud> consultarPublica(@Param("folio") String folio,
                                         @Param("curp") String curp);

    /** Acceso por la liga de correccion que se envia al observar una solicitud. */
    Optional<Solicitud> findByTokenConsulta(UUID token);

    /**
     * Bandeja de trabajo del panel, con los filtros que usa el personal.
     *
     * <p>Un parametro nulo significa "no filtrar por esto".
     */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.activo = TRUE
              AND (:estatusId IS NULL OR s.estatus.id = :estatusId)
              AND (:municipioId IS NULL OR s.persona.municipio.id = :municipioId)
              AND (:jornadaId IS NULL OR s.jornada.id = :jornadaId)
              AND (:desde IS NULL OR s.fechaRegistro >= :desde)
              AND (:hasta IS NULL OR s.fechaRegistro <= :hasta)
            """)
    @EntityGraph(attributePaths = {"persona", "animal", "estatus"})
    Page<Solicitud> buscarBandeja(@Param("estatusId") Integer estatusId,
                                  @Param("municipioId") Integer municipioId,
                                  @Param("jornadaId") Long jornadaId,
                                  @Param("desde") OffsetDateTime desde,
                                  @Param("hasta") OffsetDateTime hasta,
                                  Pageable pageable);

    /** Tramites de una persona, del mas reciente al mas antiguo. */
    Page<Solicitud> findByPersonaIdOrderByFechaRegistroDesc(Long personaId, Pageable pageable);

    long countByEstatusClave(String claveEstatus);

    /**
     * Precargas de una jornada, para la busqueda del dia del evento.
     *
     * <p>Localiza por CURP o por apellidos, ignorando acentos: el personal
     * teclea rapido y con la gente formada enfrente.
     */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.jornada.id = :jornadaId
              AND s.activo = TRUE
              AND (:texto IS NULL
                   OR s.persona.curp = :texto
                   OR sea_unaccent(UPPER(CONCAT(s.persona.apellidoPaterno, ' ',
                          COALESCE(s.persona.apellidoMaterno, ''), ' ', s.persona.nombre)))
                      LIKE sea_unaccent(UPPER(CONCAT('%', :texto, '%'))))
            """)
    @EntityGraph(attributePaths = {"persona", "animal", "estatus"})
    Page<Solicitud> buscarEnJornada(@Param("jornadaId") Long jornadaId,
                                    @Param("texto") String texto,
                                    Pageable pageable);
}
