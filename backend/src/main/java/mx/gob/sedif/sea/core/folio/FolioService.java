package mx.gob.sedif.sea.core.folio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.catalogo.Municipio;
import mx.gob.sedif.sea.core.parametro.ParametroService;

/**
 * Arma los folios del sistema.
 *
 * <p>Los patrones no estan en el codigo sino en los ajustes, porque la
 * institucion pasa de SMADSOT a DIF y el formato va a cambiar. Cambiarlo debe
 * ser editar un renglon, no recompilar y desplegar.
 *
 * <p>Marcadores admitidos:
 * <pre>
 *   {yyyy} {MM} {dd} {yyyyMMdd}   partes de la fecha
 *   {consecutivo:n}               numero con n posiciones, rellenado con ceros
 *   {cve_inegi}                   clave del municipio
 *   {dependencia} {instituto}     siglas institucionales
 * </pre>
 */
@Service
public class FolioService {

    private static final Logger log = LoggerFactory.getLogger(FolioService.class);

    public static final String TIPO_SOLICITUD = "SOLICITUD";
    public static final String TIPO_CERTIFICADO = "CERTIFICADO";
    public static final String TIPO_CITA = "CITA";

    private static final Pattern MARCADOR_CONSECUTIVO =
            Pattern.compile("\\{consecutivo:(\\d+)\\}");

    private final FolioConsecutivoRepository folioRepository;
    private final ParametroService parametroService;

    public FolioService(FolioConsecutivoRepository folioRepository,
                        ParametroService parametroService) {
        this.folioRepository = folioRepository;
        this.parametroService = parametroService;
    }

    /**
     * Folio de la solicitud, que es el que se le da a la persona.
     *
     * <p>Se conserva aunque el tramite tenga que corregirse: es el mismo
     * tramite y la persona ya lo tiene anotado.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String generarFolioSolicitud() {
        String patron = parametroService.obtener(
                "folio.solicitud.patron", "SOL-{yyyyMMdd}-{consecutivo:5}");
        long numero = siguienteConsecutivo(TIPO_SOLICITUD, null);
        return armar(patron, numero, null);
    }

    /**
     * Folio oficial del certificado, distinto del de la solicitud.
     *
     * @param municipio municipio del tutor, cuya clave INEGI va en el folio
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String generarFolioCertificado(Municipio municipio) {
        String patron = parametroService.obtener(
                "folio.certificado.patron",
                "{dependencia}/{instituto}/EMP-{cve_inegi}/{consecutivo:5}/{yyyy}");
        long numero = siguienteConsecutivo(TIPO_CERTIFICADO, municipio);
        return armar(patron, numero, municipio);
    }

    /** Numero de la cita de entrega. */
    @Transactional(propagation = Propagation.REQUIRED)
    public String generarNumeroCita() {
        String patron = parametroService.obtener(
                "folio.cita.patron", "CITA-{yyyyMMdd}-{consecutivo:4}");
        long numero = siguienteConsecutivo(TIPO_CITA, null);
        return armar(patron, numero, null);
    }

    /**
     * Identificador interno del animal.
     *
     * <p>El sistema anterior lo llamaba "CURP de mascota". No se usa ese
     * nombre: confunde con la CURP real de la persona y no tiene nada que ver
     * con RENAPO.
     *
     * @param nombreTutor     nombre del tutor
     * @param apellidoTutor   apellido paterno del tutor
     * @param nombreAnimal    nombre del animal
     * @param claveEspecie    C de canino o F de felino
     * @param claveSexo       M de macho o H de hembra
     */
    public String generarClaveAnimal(String nombreTutor, String apellidoTutor,
                                     String nombreAnimal, String claveEspecie,
                                     String claveSexo) {
        String inicialesTutor = (letraDe(apellidoTutor, 0) + letraDe(nombreTutor, 0));
        String tresDelAnimal = normalizar(nombreAnimal);
        tresDelAnimal = tresDelAnimal.length() >= 3
                ? tresDelAnimal.substring(0, 3)
                : (tresDelAnimal + "XXX").substring(0, 3);

        String especie = claveEspecie != null && claveEspecie.startsWith("FEL") ? "F" : "C";
        String sexo = claveSexo != null && claveSexo.startsWith("H") ? "H" : "M";

        return inicialesTutor + tresDelAnimal + especie + sexo
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
    }

    /**
     * Avanza el contador correspondiente y devuelve el numero.
     *
     * <p>Toma el renglon con bloqueo: si dos altas coinciden, la segunda espera
     * a que la primera termine, de modo que no puedan recibir el mismo numero.
     * Si el contador no existe todavia, se crea.
     */
    private long siguienteConsecutivo(String tipo, Municipio municipio) {
        int anio = LocalDate.now().getYear();
        Integer municipioId = municipio == null ? null : municipio.getId();

        FolioConsecutivo contador = folioRepository
                .tomarConBloqueo(tipo, anio, municipioId)
                .orElseGet(() -> {
                    FolioConsecutivo nuevo = new FolioConsecutivo();
                    nuevo.setTipo(tipo);
                    nuevo.setAnio(anio);
                    nuevo.setMunicipio(municipio);
                    nuevo.setConsecutivo(0L);
                    log.info("Se inicia el contador de {} para {}", tipo, anio);
                    return folioRepository.save(nuevo);
                });

        long numero = contador.siguiente();
        folioRepository.save(contador);
        return numero;
    }

    /** Sustituye los marcadores del patron por sus valores. */
    private String armar(String patron, long consecutivo, Municipio municipio) {
        LocalDate hoy = LocalDate.now();
        String resultado = patron;

        resultado = resultado.replace("{yyyyMMdd}",
                hoy.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        resultado = resultado.replace("{yyyy}", String.valueOf(hoy.getYear()));
        resultado = resultado.replace("{MM}",
                String.format("%02d", hoy.getMonthValue()));
        resultado = resultado.replace("{dd}",
                String.format("%02d", hoy.getDayOfMonth()));

        resultado = resultado.replace("{cve_inegi}",
                municipio == null ? "00000" : municipio.getClaveInegi());

        resultado = resultado.replace("{dependencia}",
                parametroService.obtener("folio.dependencia", "SEDIF"));
        resultado = resultado.replace("{instituto}",
                parametroService.obtener("folio.instituto", "IBA"));

        // El consecutivo se rellena con ceros hasta las posiciones indicadas.
        Matcher marcador = MARCADOR_CONSECUTIVO.matcher(resultado);
        StringBuilder salida = new StringBuilder();
        while (marcador.find()) {
            int posiciones = Integer.parseInt(marcador.group(1));
            marcador.appendReplacement(salida,
                    Matcher.quoteReplacement(
                            String.format("%0" + posiciones + "d", consecutivo)));
        }
        marcador.appendTail(salida);

        return salida.toString();
    }

    /** Letra de una posicion, en mayuscula y sin acento; X si no la hay. */
    private String letraDe(String texto, int posicion) {
        String limpio = normalizar(texto);
        return limpio.length() > posicion
                ? String.valueOf(limpio.charAt(posicion))
                : "X";
    }

    /** Mayusculas, sin acentos ni caracteres ajenos. */
    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return java.text.Normalizer
                .normalize(texto.trim().toUpperCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^A-Z0-9]", "");
    }
}
