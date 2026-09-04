package mx.gob.sedif.sea.core.documental;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacion vigente mientras no haya firma electronica avanzada.
 *
 * <p>No firma nada y lo dice con claridad. Deliberadamente no simula una firma:
 * un documento que aparente estar firmado sin estarlo seria peor que uno que
 * declara no estarlo, porque induciria a confiar en algo que no tiene validez.
 *
 * <p>El documento sale igual, con su codigo de verificacion y la leyenda que
 * indica donde acudir por el sello institucional.
 */
@Service
@ConditionalOnProperty(name = "app.firma.habilitada", havingValue = "false",
                       matchIfMissing = true)
public class FirmaServiceStub implements FirmaService {

    private static final Logger log = LoggerFactory.getLogger(FirmaServiceStub.class);

    private final String leyendaSelloFisico;

    public FirmaServiceStub(
            @Value("${app.firma.leyenda-sello-fisico:"
                    + "Si requieres el sello institucional impreso, acude a las oficinas "
                    + "del Instituto de Bienestar Animal.}") String leyendaSelloFisico) {
        this.leyendaSelloFisico = leyendaSelloFisico;
    }

    @Override
    public ResultadoFirma firmar(byte[] documento, ContextoFirma contexto) {
        log.info("Certificado {} emitido sin firma electronica; se entrega con "
                        + "código de verificación y leyenda de sello fisico",
                contexto.folioCertificado());

        return ResultadoFirma.sinFirmar(documento, leyendaSelloFisico);
    }

    @Override
    public boolean estaDisponible() {
        return false;
    }
}
