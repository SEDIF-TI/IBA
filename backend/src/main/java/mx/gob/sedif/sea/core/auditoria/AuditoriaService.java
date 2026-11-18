package mx.gob.sedif.sea.core.auditoria;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper json;

    public AuditoriaService(BitacoraAuditoriaRepository bitacoraRepository,
                            UsuarioRepository usuarioRepository,
                            ObjectMapper json) {
        this.bitacoraRepository = bitacoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.json = json;
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
     * Registra una modificación con el estado anterior y el nuevo, convertidos a JSON.
     *
     * <p>Regla: los mapas solo deben llevar datos que no sean personales (CURP, domicilio,
     * teléfonos, correo de la ciudadanía). Para registros con datos personales se usa
     * {@link #registrar(String, String, Long, String)} con los nombres de los campos cambiados.
     *
     * @param accion     constante de {@link AccionAuditoria}
     * @param entidad    nombre de la tabla o concepto afectado
     * @param registroId identificador del registro
     * @param detalle    descripción legible del cambio
     * @param antes      valores previos, campo por campo; puede ser nulo
     * @param despues    valores resultantes, campo por campo; puede ser nulo
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrarCambio(String accion, String entidad, Long registroId, String detalle,
                                Map<String, ?> antes, Map<String, ?> despues) {
        registrar(accion, entidad, registroId, detalle, aJson(antes), aJson(despues));
    }

    /**
     * Registra una petición al API; lo invoca {@link RegistroActividadFilter} al terminar cada una.
     *
     * @param actividad datos de la petición, ya depurados (sin valores de búsqueda ni cuerpos)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrarActividad(Actividad actividad) {
        try {
            BitacoraAuditoria registro = new BitacoraAuditoria();
            registro.setAccion(actividad.accion());
            registro.setEntidad(actividad.entidad());
            registro.setRegistroId(actividad.registroId());
            registro.setDetalle(actividad.detalle());
            registro.setMetodo(actividad.metodo());
            registro.setRuta(actividad.ruta());
            registro.setCodigoHttp((short) actividad.codigoHttp());
            registro.setResultado(actividad.resultado());
            registro.setDuracionMs((int) Math.min(Integer.MAX_VALUE, actividad.duracionMs()));
            if (actividad.usuarioId() != null) {
                registro.setUsuario(usuarioRepository.getReferenceById(actividad.usuarioId()));
            }
            registro.setIp(actividad.ip());
            registro.setUserAgent(actividad.userAgent());
            bitacoraRepository.save(registro);
        } catch (Exception ex) {
            log.error("No se pudo registrar en bitacora la peticion {} {}",
                    actividad.metodo(), actividad.ruta(), ex);
        }
    }

    /**
     * Petición al API lista para la bitácora.
     *
     * @param usuarioId  cuenta en sesión; nulo en el portal público o sin sesión válida
     * @param accion     CONSULTA, ALTA, EDICION, BAJA, ACCESO_DENEGADO, NO_AUTENTICADO o ERROR
     * @param entidad    recurso del API (primer segmento de la ruta)
     * @param registroId primer identificador numérico de la ruta, si lo hay
     * @param metodo     método HTTP
     * @param ruta       ruta sin parámetros de consulta
     * @param codigoHttp código de la respuesta
     * @param resultado  EXITO, RECHAZADO, DENEGADO o ERROR
     * @param duracionMs milisegundos que tardó la petición
     * @param ip         IP real del cliente
     * @param userAgent  navegador, recortado a 400 caracteres; puede ser nulo
     * @param detalle    resumen legible; solo nombres de filtros, nunca sus valores
     */
    public record Actividad(Long usuarioId, String accion, String entidad, Long registroId,
                            String metodo, String ruta, int codigoHttp, String resultado,
                            long duracionMs, String ip, String userAgent, String detalle) {
    }

    /** Convierte un mapa a JSON para las columnas jsonb; nulo si no hay mapa o falla. */
    private String aJson(Map<String, ?> valores) {
        if (valores == null) {
            return null;
        }
        try {
            return json.writeValueAsString(valores);
        } catch (JsonProcessingException ex) {
            log.warn("No se pudo convertir a JSON el estado para la bitacora: {}", ex.getMessage());
            return null;
        }
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
        guardar(SecurityUtil.obtenerUsuarioId().orElse(null),
                accion, entidad, registroId, detalle, valorAnterior, valorNuevo);
    }

    /**
     * Registra una acción a nombre de una cuenta concreta, aunque todavía no haya sesión.
     *
     * <p>Lo usan los eventos de acceso (inicio de sesión, intento fallido, bloqueo, cierre), que
     * ocurren antes de que exista un usuario autenticado en el contexto de seguridad.
     *
     * @param usuarioId  cuenta a la que se atribuye el movimiento; nulo si no se pudo identificar
     * @param accion     constante de {@link AccionAuditoria}
     * @param entidad    nombre de la tabla o concepto afectado
     * @param registroId identificador del registro afectado, si aplica
     * @param detalle    descripción legible; nunca contraseñas, tokens ni datos personales
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void registrarComo(Long usuarioId, String accion, String entidad, Long registroId,
                              String detalle) {
        guardar(usuarioId, accion, entidad, registroId, detalle, null, null);
    }

    /**
     * Escribe el registro. Nunca lanza excepción: un fallo de bitácora se anota en el log del
     * servidor y no interrumpe la operación principal.
     */
    private void guardar(Long usuarioId, String accion, String entidad, Long registroId,
                         String detalle, String valorAnterior, String valorNuevo) {
        try {
            BitacoraAuditoria registro = new BitacoraAuditoria();
            registro.setAccion(accion);
            registro.setEntidad(entidad);
            registro.setRegistroId(registroId);
            registro.setDetalle(detalle);
            registro.setValorAnterior(valorAnterior);
            registro.setValorNuevo(valorNuevo);

            // Queda nulo cuando la accion viene del portal publico, sin sesion.
            if (usuarioId != null) {
                usuarioRepository.findById(usuarioId).ifPresent(registro::setUsuario);
            }

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
