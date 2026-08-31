package mx.gob.sedif.sea.core.documento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los metadatos de los archivos adjuntos. */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @EntityGraph(attributePaths = {"tipoDocumento"})
    List<Documento> findBySolicitudIdAndActivoTrue(Long solicitudId);

    /** Localiza por el nombre generado en disco, para servir la descarga. */
    Optional<Documento> findByNombreAlmacenado(String nombreAlmacenado);

    /** Detecta que el mismo archivo ya se subio antes. */
    List<Documento> findByHashSha256(String hash);

    /** Documentos marcados como ilegibles: hay que pedir reenvio. */
    List<Documento> findBySolicitudIdAndLegibleFalse(Long solicitudId);
}
