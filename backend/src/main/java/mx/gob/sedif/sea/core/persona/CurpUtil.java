package mx.gob.sedif.sea.core.persona;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lectura y comprobacion de la CURP.
 *
 * <p>La CURP no es un dato opaco: su composicion permite derivar fecha de
 * nacimiento, sexo y entidad de registro, y cotejar que las iniciales
 * correspondan con el nombre capturado. Eso sirve para detectar errores de
 * captura y, sobre todo, intentos de registrar a nombre de otra persona: como
 * la ciudadania no se autentica, este cotejo es uno de los pocos controles
 * disponibles.
 *
 * <p>Estructura de las 18 posiciones:
 * <pre>
 *   1-4    iniciales del apellido paterno, materno y nombre
 *   5-10   fecha de nacimiento (AAMMDD)
 *   11     sexo (H o M)
 *   12-13  entidad de registro
 *   14-16  consonantes internas
 *   17     homoclave (digito o letra)
 *   18     digito verificador
 * </pre>
 */
public final class CurpUtil {

    private CurpUtil() {
    }

    private static final Pattern FORMATO = Pattern.compile(
            "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z][0-9]$");

    /** Alfabeto con el que se calcula el digito verificador. */
    private static final String ALFABETO = "0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";

    /** Claves de entidad validas, incluida NE para nacidos en el extranjero. */
    private static final Set<String> ENTIDADES = Set.of(
            "AS", "BC", "BS", "CC", "CL", "CM", "CS", "CH", "DF", "DG",
            "GT", "GR", "HG", "JC", "MC", "MN", "MS", "NT", "NL", "OC",
            "PL", "QT", "QR", "SP", "SL", "SR", "TC", "TS", "TL", "VZ",
            "YN", "ZS", "NE");

    /** Nombres de las entidades, para mostrarlos en pantalla. */
    private static final Map<String, String> NOMBRES_ENTIDAD = Map.ofEntries(
            Map.entry("AS", "Aguascalientes"), Map.entry("BC", "Baja California"),
            Map.entry("BS", "Baja California Sur"), Map.entry("CC", "Campeche"),
            Map.entry("CL", "Coahuila"), Map.entry("CM", "Colima"),
            Map.entry("CS", "Chiapas"), Map.entry("CH", "Chihuahua"),
            Map.entry("DF", "Ciudad de Mexico"), Map.entry("DG", "Durango"),
            Map.entry("GT", "Guanajuato"), Map.entry("GR", "Guerrero"),
            Map.entry("HG", "Hidalgo"), Map.entry("JC", "Jalisco"),
            Map.entry("MC", "Estado de Mexico"), Map.entry("MN", "Michoacan"),
            Map.entry("MS", "Morelos"), Map.entry("NT", "Nayarit"),
            Map.entry("NL", "Nuevo Leon"), Map.entry("OC", "Oaxaca"),
            Map.entry("PL", "Puebla"), Map.entry("QT", "Queretaro"),
            Map.entry("QR", "Quintana Roo"), Map.entry("SP", "San Luis Potosi"),
            Map.entry("SL", "Sinaloa"), Map.entry("SR", "Sonora"),
            Map.entry("TC", "Tabasco"), Map.entry("TS", "Tamaulipas"),
            Map.entry("TL", "Tlaxcala"), Map.entry("VZ", "Veracruz"),
            Map.entry("YN", "Yucatan"), Map.entry("ZS", "Zacatecas"),
            Map.entry("NE", "Nacido en el extranjero"));

    /**
     * Comprueba formato, entidad, fecha y digito verificador.
     *
     * @param curp valor a revisar
     * @return verdadero si la CURP es valida
     */
    public static boolean esValida(String curp) {
        if (curp == null) {
            return false;
        }
        String limpia = normalizar(curp);

        if (!FORMATO.matcher(limpia).matches()) {
            return false;
        }
        if (!ENTIDADES.contains(limpia.substring(11, 13))) {
            return false;
        }
        if (extraerFechaNacimiento(limpia).isEmpty()) {
            return false;
        }
        return digitoVerificadorCorrecto(limpia);
    }

    /**
     * Comprueba solo la forma, sin el digito verificador.
     *
     * <p>Sirve para avisar en pantalla mientras la persona escribe, sin
     * rechazar todavia.
     */
    public static boolean tieneFormatoValido(String curp) {
        return curp != null && FORMATO.matcher(normalizar(curp)).matches();
    }

    /**
     * Fecha de nacimiento contenida en la CURP.
     *
     * <p>El siglo se deduce de la posicion 17: las CURP del siglo XX llevan ahi
     * un digito, y las del XXI una letra.
     */
    public static Optional<LocalDate> extraerFechaNacimiento(String curp) {
        if (curp == null || curp.length() < 17) {
            return Optional.empty();
        }
        String limpia = normalizar(curp);
        try {
            int anio = Integer.parseInt(limpia.substring(4, 6));
            int mes = Integer.parseInt(limpia.substring(6, 8));
            int dia = Integer.parseInt(limpia.substring(8, 10));

            char homoclave = limpia.charAt(16);
            int siglo = Character.isDigit(homoclave) ? 1900 : 2000;

            LocalDate fecha = LocalDate.of(siglo + anio, mes, dia);

            // Una fecha posterior a hoy delata un error de captura.
            if (fecha.isAfter(LocalDate.now())) {
                return Optional.empty();
            }
            return Optional.of(fecha);

        } catch (NumberFormatException | java.time.DateTimeException ex) {
            // DateTimeException cubre tambien las fechas imposibles, como el 31
            // de febrero, que LocalDate.of rechaza.
            return Optional.empty();
        }
    }

    /** Sexo registrado en la CURP: H o M. */
    public static Optional<String> extraerSexo(String curp) {
        if (curp == null || curp.length() < 11) {
            return Optional.empty();
        }
        String sexo = String.valueOf(normalizar(curp).charAt(10));
        return ("H".equals(sexo) || "M".equals(sexo)) ? Optional.of(sexo) : Optional.empty();
    }

    /** Clave de la entidad de registro. */
    public static Optional<String> extraerEntidad(String curp) {
        if (curp == null || curp.length() < 13) {
            return Optional.empty();
        }
        String entidad = normalizar(curp).substring(11, 13);
        return ENTIDADES.contains(entidad) ? Optional.of(entidad) : Optional.empty();
    }

    /** Nombre de la entidad, para mostrarlo. */
    public static Optional<String> nombreEntidad(String claveEntidad) {
        return Optional.ofNullable(NOMBRES_ENTIDAD.get(claveEntidad));
    }

    /**
     * Coteja que las cuatro primeras letras correspondan con el nombre.
     *
     * <p>Si no coinciden, la solicitud se marca para revision reforzada. No se
     * bloquea: hay apellidos compuestos y casos que la regla general no cubre,
     * y rechazar de plano dejaria fuera a personas con nombres legitimos.
     *
     * @param curp            CURP capturada
     * @param nombre          nombre de pila
     * @param apellidoPaterno primer apellido
     * @param apellidoMaterno segundo apellido, puede faltar
     * @return verdadero si las iniciales concuerdan
     */
    public static boolean coincideConNombre(String curp, String nombre,
                                            String apellidoPaterno, String apellidoMaterno) {
        if (curp == null || curp.length() < 4 || nombre == null || apellidoPaterno == null) {
            return false;
        }

        String limpia = normalizar(curp);
        String esperada = construirIniciales(nombre, apellidoPaterno, apellidoMaterno);

        return limpia.substring(0, 4).equals(esperada);
    }

    /**
     * Arma las cuatro letras iniciales segun la regla de RENAPO.
     *
     * <p>Primera letra y primera vocal interna del apellido paterno, inicial
     * del materno e inicial del nombre.
     */
    public static String construirIniciales(String nombre, String apellidoPaterno,
                                            String apellidoMaterno) {
        String paterno = limpiarTexto(apellidoPaterno);
        String materno = limpiarTexto(apellidoMaterno);
        String pila = limpiarTexto(primerNombreUtil(nombre));

        if (paterno.isEmpty() || pila.isEmpty()) {
            return "";
        }

        char inicialPaterno = paterno.charAt(0);
        char vocalPaterno = primeraVocalInterna(paterno);
        // Sin apellido materno se usa X, como hace RENAPO.
        char inicialMaterno = materno.isEmpty() ? 'X' : materno.charAt(0);
        char inicialNombre = pila.charAt(0);

        return "" + inicialPaterno + vocalPaterno + inicialMaterno + inicialNombre;
    }

    /**
     * Comprueba el digito verificador.
     *
     * <p>Se suma el valor posicional de los primeros 17 caracteres y el
     * resultado debe cuadrar con el ultimo.
     */
    public static boolean digitoVerificadorCorrecto(String curp) {
        if (curp == null || curp.length() != 18) {
            return false;
        }
        String limpia = normalizar(curp);
        int suma = 0;

        for (int i = 0; i < 17; i++) {
            int valor = ALFABETO.indexOf(limpia.charAt(i));
            if (valor < 0) {
                return false;
            }
            suma += valor * (18 - i);
        }

        int digitoEsperado = (10 - (suma % 10)) % 10;
        return digitoEsperado == Character.getNumericValue(limpia.charAt(17));
    }

    /**
     * Descarta los nombres de pila que RENAPO ignora.
     *
     * <p>Para "Maria Guadalupe" se toma Guadalupe, porque Maria y Jose son tan
     * frecuentes como primer nombre que la regla los omite si hay otro.
     */
    private static String primerNombreUtil(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length > 1) {
            String primero = limpiarTexto(partes[0]);
            if (primero.equals("MARIA") || primero.equals("JOSE")
                    || primero.equals("MA") || primero.equals("MA.")
                    || primero.equals("J")) {
                return partes[1];
            }
        }
        return partes[0];
    }

    /** Primera vocal despues de la inicial; X si no hay ninguna. */
    private static char primeraVocalInterna(String texto) {
        for (int i = 1; i < texto.length(); i++) {
            char letra = texto.charAt(i);
            if ("AEIOU".indexOf(letra) >= 0) {
                return letra;
            }
        }
        return 'X';
    }

    /** Deja el texto en mayusculas, sin acentos ni caracteres ajenos. */
    private static String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = java.text.Normalizer
                .normalize(texto.trim().toUpperCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // La enie se conserva: forma parte del alfabeto de la CURP.
        return sinAcentos.replaceAll("[^A-ZÑ]", "");
    }

    /** Deja la CURP en mayusculas y sin espacios. */
    public static String normalizar(String curp) {
        return curp == null ? null : curp.trim().toUpperCase().replaceAll("\\s", "");
    }
}
