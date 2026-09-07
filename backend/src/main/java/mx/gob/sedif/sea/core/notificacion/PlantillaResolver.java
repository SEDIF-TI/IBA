package mx.gob.sedif.sea.core.notificacion;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.cita.Cita;
import mx.gob.sedif.sea.core.solicitud.Solicitud;

/**
 * Sustituye los marcadores de una plantilla por los datos del tramite.
 *
 * <p>Las plantillas las edita el personal desde el panel, asi que puede haber
 * marcadores mal escritos o que ya no existen. En ese caso el marcador se deja
 * tal cual y se anota en la bitacora del servidor: es preferible un correo con
 * un hueco visible que uno que no se envia, o peor, uno donde el hueco pasa
 * inadvertido.
 */
@Component
public class PlantillaResolver {

    private static final Logger log = LoggerFactory.getLogger(PlantillaResolver.class);

    private static final Pattern MARCADOR = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm 'horas'",
                    new Locale("es", "MX"));

    /** Dias que dura la liga de correccion, cuando no se indica otra cosa. */
    private static final int DIAS_CORRECCION_POR_OMISION = 15;

    private int diasVigenciaPorOmision() {
        return DIAS_CORRECCION_POR_OMISION;
    }

    /**
     * Resuelve una plantilla con los datos disponibles.
     *
     * @param plantilla texto con marcadores
     * @param datos     valores por nombre de marcador
     * @return texto resuelto
     */
    public String resolver(String plantilla, Map<String, String> datos) {
        if (plantilla == null || plantilla.isBlank()) {
            return "";
        }

        Matcher marcador = MARCADOR.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();

        while (marcador.find()) {
            String nombre = marcador.group(1);
            String valor = datos.get(nombre);

            if (valor == null) {
                // Se deja el marcador visible: quien reciba el correo vera que
                // falta un dato, y el personal podra corregir la plantilla.
                log.warn("La plantilla usa el marcador '{{{}}}' y no hay dato para el",
                        nombre);
                valor = marcador.group(0);
            }

            marcador.appendReplacement(resultado, Matcher.quoteReplacement(valor));
        }
        marcador.appendTail(resultado);

        return resultado.toString();
    }

    /**
     * Datos de un tramite, listos para sustituir en una plantilla.
     *
     * @param solicitud   tramite del que se toman los datos
     * @param certificado certificado emitido, si ya existe
     * @param cita        cita asignada, si ya existe
     * @param urlConsulta direccion del seguimiento publico
     */
    public Map<String, String> datosDe(Solicitud solicitud, Certificado certificado,
                                       Cita cita, String urlConsulta) {
        return datosDe(solicitud, certificado, cita, urlConsulta, diasVigenciaPorOmision());
    }

    /**
     * Datos de un tramite, indicando cuantos dias dura la liga de correccion.
     *
     * @param diasVigenciaCorreccion dias que la persona tiene para corregir
     */
    public Map<String, String> datosDe(Solicitud solicitud, Certificado certificado,
                                       Cita cita, String urlConsulta,
                                       int diasVigenciaCorreccion) {
        Map<String, String> datos = new HashMap<>();

        datos.put("folio", solicitud.getFolioSolicitud());
        datos.put("nombre", solicitud.getPersona().getNombre());
        datos.put("nombre_completo", solicitud.getPersona().getNombreCompleto());
        datos.put("nombre_animal", solicitud.getAnimal().getNombre());
        datos.put("estatus", solicitud.getEstatus().getNombre());
        datos.put("fecha_registro", solicitud.getFechaRegistro().format(FECHA_HORA));
        datos.put("url_consulta", urlConsulta);
        datos.put("url_correccion", urlConsulta);
        datos.put("dias_vigencia", String.valueOf(diasVigenciaCorreccion));

        if (solicitud.getAnimal().getEspecie() != null) {
            datos.put("especie", solicitud.getAnimal().getEspecie().getNombre());
        }
        if (solicitud.getPersona().getMunicipio() != null) {
            datos.put("municipio", solicitud.getPersona().getMunicipio().getNombre());
        }
        if (solicitud.getMotivoRechazo() != null) {
            datos.put("motivo", solicitud.getMotivoRechazo().getNombre());
        }
        if (solicitud.getObservacionResolucion() != null) {
            datos.put("observación", solicitud.getObservacionResolucion());
        }

        if (certificado != null) {
            datos.put("folio_certificado", certificado.getFolio());
            datos.put("codigo_verificacion", certificado.getCodigoVerificacion().toString());
            if (certificado.getFechaVigencia() != null) {
                datos.put("vigencia", certificado.getFechaVigencia().format(FECHA));
            }
        }

        if (cita != null) {
            datos.put("numero_cita", cita.getNumeroCita());
            datos.put("fecha_cita", cita.getFechaCita().format(FECHA));
            if (cita.getHorario() != null) {
                datos.put("horario_cita", cita.getHorario());
            }
            if (cita.getSede() != null) {
                datos.put("sede_nombre", cita.getSede().getNombre());
                datos.put("sede_domicilio", cita.getSede().getDomicilio());
                if (cita.getSede().getHorario() != null) {
                    datos.put("sede_horario", cita.getSede().getHorario());
                }
                if (cita.getSede().getTelefono() != null) {
                    datos.put("sede_telefono", cita.getSede().getTelefono());
                }
            }
        }

        return datos;
    }

    /**
     * Marcadores que quedaron sin resolver en un texto.
     *
     * <p>Sirve para avisar al personal cuando edita una plantilla, antes de que
     * el correo salga con huecos.
     */
    public java.util.List<String> marcadoresSinResolver(String texto) {
        java.util.List<String> pendientes = new java.util.ArrayList<>();
        Matcher marcador = MARCADOR.matcher(texto);
        while (marcador.find()) {
            pendientes.add(marcador.group(1));
        }
        return pendientes;
    }
}
