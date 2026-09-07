package mx.gob.sedif.sea.core.auditoria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Deja constancia de las acciones sensibles del sistema.
 *
 * <p>Cada registro se escribe en una transaccion propia y separada. Es
 * deliberado, por dos motivos:
 * <ul>
 *   <li>Si la operacion auditada falla y se revierte, la constancia queda
 *       igual. Un intento de descarga que reviento tambien interesa saberlo.</li>
 *   <li>Si el registro falla, no arrastra a la operacion principal. Atrapar la
 *       excepcion no basta: dentro de la transaccion del llamador, un error de
 *       base la deja marcada para revertir y la operacion se pierde igual
 *       aunque nadie vuelva a lanzar nada.</li>
 * </ul>
 *
 * <p>Perder un registro de auditoria es malo; impedir que el personal trabaje
 * por eso, peor.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final BitacoraAuditoriaRepository bitacoraRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(BitacoraAuditoriaRepository bitacoraRepository,
                            UsuarioRepository usuarioRepository) {
        this.bitacoraRepository = bitacoraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra una accion.
     *
     * @param accion      que se hizo; usar las constantes de {@link AccionAuditoria}
     * @param entidad     nombre de la tabla o concepto afectado
     * @param registroId  identificador del registro afectado, si aplica
     * @param detalle     descripcion legible para la pantalla de consulta
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrar(String accion, String entidad, Long registroId, String detalle) {
        registrar(accion, entidad, registroId, detalle, null, null);
    }

    /**
     * Registra una accion incluyendo el estado antes y despues del cambio.
     *
     * @param valorAnterior estado previo en JSON, o nulo
     * @param valorNuevo    estado resultante en JSON, o nulo
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrar(String accion, String entidad, Long registroId, String detalle,
                          String valorAnterior, String valorNuevo) {
        try {
            BitacoraAuditoria registro = new BitacoraAuditoria();
            registro.setAccion(accion);
            registro.setEntidad(entidad);
            registro.setRegistroId(registroId);
            registro.setDetalle(detalle);
            registro.setValorAnterior(valorAnterior);
            registro.setValorNuevo(valorNuevo);

            // Queda nulo cuando la accion viene del portal publico, sin sesion.
            SecurityUtil.obtenerUsuarioId()
                    .flatMap(usuarioRepository::findById)
                    .ifPresent(registro::setUsuario);

            obtenerPeticionActual().ifPresent(peticion -> {
                registro.setIp(SecurityUtil.obtenerIpCliente(peticion));
                registro.setUserAgent(SecurityUtil.obtenerUserAgent(peticion));
            });

            bitacoraRepository.save(registro);

        } catch (Exception ex) {
            // Nunca dejar que un fallo de auditoria interrumpa la operacion.
            log.error("No se pudo registrar en bitacora la accion {} sobre {} {}",
                    accion, entidad, registroId, ex);
        }
    }

    /**
     * Registra una consulta por CURP.
     *
     * <p>Tiene metodo propio porque es de los movimientos que mas importa
     * vigilar: permite saber quien anduvo buscando los datos de una persona.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrarConsultaCurp(String curp, Long personaId, boolean encontrada) {
        // La CURP se guarda enmascarada: la bitacora no debe convertirse en otro
        // sitio del que extraer datos personales completos.
        String curpEnmascarada = enmascarar(curp);
        registrar(AccionAuditoria.CONSULTAR_CURP,
                "persona",
                personaId,
                "Consulta por CURP %s (%s)".formatted(
                        curpEnmascarada, encontrada ? "encontrada" : "sin resultados"));
    }

    /**
     * Registra la descarga de un documento personal.
     *
     * <p>Es el registro que exige el requisito de seguridad: cada vez que
     * alguien accede a un INE o comprobante, queda dicho quien y cuando.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrarDescargaDocumento(Long documentoId, String tipoDocumento,
                                           Long solicitudId) {
        registrar(AccionAuditoria.DESCARGAR_DOCUMENTO,
                "documento",
                documentoId,
                "Descarga de %s del trámite %d".formatted(tipoDocumento, solicitudId));
    }

    /** Deja ver solo el inicio y el final de la CURP. */
    private String enmascarar(String curp) {
        if (curp == null || curp.length() != 18) {
            return "(invalida)";
        }
        return curp.substring(0, 4) + "**********" + curp.substring(16);
    }

    /** Peticion HTTP en curso, si la hay. */
    private java.util.Optional<HttpServletRequest> obtenerPeticionActual() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes atributos) {
            return java.util.Optional.of(atributos.getRequest());
        }
        return java.util.Optional.empty();
    }
}
