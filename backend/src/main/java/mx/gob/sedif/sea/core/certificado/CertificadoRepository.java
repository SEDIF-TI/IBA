package mx.gob.sedif.sea.core.certificado;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<Certificado> findByCodigoVerificacion(UUID codigo);

    Optional<Certificado> findByFolio(String folio);

    Optional<Certificado> findBySolicitudId(Long solicitudId);

    boolean existsBySolicitudId(Long solicitudId);
}
