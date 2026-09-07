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
    /**
     * Bandeja de trabajo del panel, con los filtros que usa el personal.
     *
     * <p>Las fechas se comparan contra un rango completo en lugar de admitir
     * nulos: PostgreSQL no puede deducir el tipo de un parametro nulo suelto y
     * rechaza la consulta. Quien llama sustituye el nulo por el extremo del
     * rango, con lo que el filtro deja de restringir sin necesidad de nulos.
     */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.activo = TRUE
              AND (:estatusId IS NULL OR s.estatus.id = :estatusId)
              AND (:municipioId IS NULL OR s.persona.municipio.id = :municipioId)
              AND (:jornadaId IS NULL OR s.jornada.id = :jornadaId)
              AND s.fechaRegistro BETWEEN :desde AND :hasta
            """)
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal",
                                   "animal.especie", "estatus"})
    Page<Solicitud> buscarEnRango(@Param("estatusId") Integer estatusId,
                                  @Param("municipioId") Integer municipioId,
                                  @Param("jornadaId") Long jornadaId,
                                  @Param("desde") OffsetDateTime desde,
                                  @Param("hasta") OffsetDateTime hasta,
                                  Pageable pageable);

    /**
     * Bandeja con filtros opcionales.
     *
     * <p>Un filtro sin valor no restringe: las fechas ausentes se sustituyen
     * por extremos que abarcan cualquier registro.
     */
    default Page<Solicitud> buscarBandeja(Integer estatusId, Integer municipioId,
                                          Long jornadaId, OffsetDateTime desde,
                                          OffsetDateTime hasta, Pageable pageable) {
        OffsetDateTime inicio = desde != null
                ? desde
                : OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        OffsetDateTime fin = hasta != null
                ? hasta
                : OffsetDateTime.now().plusYears(100);

        return buscarEnRango(estatusId, municipioId, jornadaId, inicio, fin, pageable);
    }

    /**
     * Tramites de una persona, del mas reciente al mas antiguo.
     *
     * <p>Trae estatus y animal en la misma consulta: la ficha del tutor arma el
     * DTO fuera de la transaccion y sin esto falla al leer el estatus.
     */
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal",
                                   "animal.especie", "estatus"})
    Page<Solicitud> findByPersonaIdOrderByFechaRegistroDesc(Long personaId, Pageable pageable);

    long countByEstatusClave(String claveEstatus);

    /** Cuantos tramites vigentes tiene una persona. */
    long countByPersonaIdAndActivoTrue(Long personaId);

    /**
     * Tramites dados de baja, para poder revisarlos y reactivarlos.
     *
     * <p>La baja es logica: el registro sigue ahi, solo deja de aparecer en la
     * bandeja de trabajo.
     */
    @Query("SELECT s FROM Solicitud s WHERE s.activo = FALSE ORDER BY s.fechaRegistro DESC")
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal",
                                   "animal.especie", "estatus"})
    Page<Solicitud> findDadosDeBaja(Pageable pageable);

    /** Cuantas precargas tiene una jornada. */
    long countByJornadaId(Long jornadaId);

    /** Cuantas precargas de una jornada llegaron a alguna de las situaciones dadas. */
    long countByJornadaIdAndEstatusClaveIn(Long jornadaId, java.util.List<String> claves);

    /**
     * Precargas de una jornada, para la busqueda del dia del evento.
     *
     * <p>Localiza por CURP o por apellidos, ignorando acentos: el personal
     * teclea rapido y con la gente formada enfrente.
     */
    /**
     * Precargas de una jornada que coinciden con el texto buscado.
     *
     * <p>El patron llega ya en mayusculas y con comodines, y vacio en lugar de
     * nulo: PostgreSQL no puede deducir el tipo de un parametro nulo dentro de
     * UPPER() y rechazaria la consulta.
     */
    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.jornada.id = :jornadaId
              AND s.activo = TRUE
              AND (:patron = ''
                   OR s.persona.curp = :curp
                   OR sea_unaccent(UPPER(CONCAT(s.persona.apellidoPaterno, ' ',
                          COALESCE(s.persona.apellidoMaterno, ''), ' ', s.persona.nombre)))
                      LIKE sea_unaccent(:patron))
            """)
    @EntityGraph(attributePaths = {"persona", "persona.municipio", "animal",
                                   "animal.especie", "estatus"})
    Page<Solicitud> buscarEnJornadaPorPatron(@Param("jornadaId") Long jornadaId,
                                             @Param("patron") String patron,
                                             @Param("curp") String curp,
                                             Pageable pageable);

    /**
     * Busca a una persona entre las precargas de una jornada.
     *
     * <p>Es la consulta del dia del evento: admite CURP completa o parte del
     * nombre, e ignora acentos.
     *
     * @param texto CURP, apellidos o nombre; vacio devuelve todas
     */
    default Page<Solicitud> buscarEnJornada(Long jornadaId, String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return buscarEnJornadaPorPatron(jornadaId, "", "", pageable);
        }
        String limpio = texto.trim().toUpperCase();
        return buscarEnJornadaPorPatron(jornadaId, "%" + limpio + "%", limpio, pageable);
    }
}
