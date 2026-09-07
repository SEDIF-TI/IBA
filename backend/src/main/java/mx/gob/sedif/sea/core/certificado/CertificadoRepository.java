package mx.gob.sedif.sea.core.certificado;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a datos de los certificados emitidos. */
@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    /**
     * Destino del QR: verificacion publica del documento.
     *
     * <p>La respuesta muestra la validez del certificado y los datos del
     * animal, nunca informacion personal del tutor.
     */
    @Query("SELECT c FROM Certificado c WHERE c.codigoVerificacion = :codigo")
    @EntityGraph(attributePaths = {
            "solicitud", "solicitud.animal", "solicitud.animal.especie",
            "solicitud.animal.raza", "solicitud.animal.sexo", "solicitud.animal.color",
            "solicitud.persona", "solicitud.persona.municipio"})
    Optional<Certificado> findByCodigoVerificacion(@Param("código") UUID codigo);

    @Query("SELECT c FROM Certificado c WHERE c.folio = :folio")
    @EntityGraph(attributePaths = {"solicitud", "solicitud.animal", "solicitud.persona"})
    Optional<Certificado> findByFolio(@Param("folio") String folio);

    @Query("SELECT c FROM Certificado c WHERE c.solicitud.id = :solicitudId")
    @EntityGraph(attributePaths = {
            "solicitud", "solicitud.animal", "solicitud.animal.especie",
            "solicitud.persona", "solicitud.persona.municipio"})
    Optional<Certificado> findBySolicitudId(@Param("solicitudId") Long solicitudId);

    /**
     * Certificado con todo lo que hace falta para componer sus documentos.
     *
     * <p>Se traen las relaciones a proposito: el PDF y los DTO se arman fuera
     * de la transaccion, donde una relacion sin cargar ya no se puede resolver.
     */
    @Query("SELECT c FROM Certificado c WHERE c.id = :id")
    @EntityGraph(attributePaths = {
            "solicitud", "solicitud.animal", "solicitud.animal.especie",
            "solicitud.animal.raza", "solicitud.animal.sexo", "solicitud.animal.color",
            "solicitud.animal.tamano", "solicitud.animal.metodoIdentificacion",
            "solicitud.persona", "solicitud.persona.municipio", "usuarioEmisor"})
    Optional<Certificado> findConDetallePorId(@Param("id") Long id);

    boolean existsBySolicitudId(Long solicitudId);
}
