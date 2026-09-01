package mx.gob.sedif.sea.core.documental;

import java.awt.Color;

import org.springframework.stereotype.Component;

import mx.gob.sedif.sea.core.parametro.ParametroService;

/**
 * Identidad institucional de los documentos.
 *
 * <p>Se lee de los ajustes, no del codigo: el sistema pasa de SMADSOT a DIF y
 * con el cambian nombre, siglas y colores. Que eso obligue a recompilar seria
 * absurdo.
 */
@Component
public class IdentidadGrafica {

    private final ParametroService parametros;

    public IdentidadGrafica(ParametroService parametros) {
        this.parametros = parametros;
    }

    /** Institucion que expide el documento. */
    public String nombreInstitucion() {
        return parametros.obtener("institucion.nombre", "Sistema Estatal DIF Puebla");
    }

    /** Area responsable del padron. */
    public String nombreArea() {
        return parametros.obtener("institucion.area", "Instituto de Bienestar Animal");
    }

    /** Siglas del area, para encabezados y folios. */
    public String siglas() {
        return parametros.obtener("institucion.siglas", "IBA");
    }

    /** Color institucional, usado en titulos y filetes. */
    public Color colorPrimario() {
        return desdeHex(parametros.obtener("institucion.color_primario", "#801A36"));
    }

    // ---- Colores de los documentos ----
    //
    // Van aparte del color institucional porque los formatos oficiales del IBA
    // usan una paleta que todavia no esta definida al cien por ciento. Al ser
    // ajustes, cambiarla no exige recompilar. Por omision, el guinda vigente.

    /** Color del titulo del certificado y la credencial. */
    public Color colorTitulo() {
        return desdeHex(parametros.obtener("documento.color_titulo", "#801A36"));
    }

    /** Color de las etiquetas de los campos. */
    public Color colorEtiquetas() {
        return desdeHex(parametros.obtener("documento.color_etiquetas", "#801A36"));
    }

    /** Color de la franja institucional superior. */
    public Color colorFranja() {
        return desdeHex(parametros.obtener("documento.color_franja", "#801A36"));
    }

    /** Color de la cinta con el nombre del padron. */
    public Color colorCinta() {
        return desdeHex(parametros.obtener("documento.color_cinta", "#64748B"));
    }

    /** Color del fondo decorativo de huellas. */
    public Color colorFondoHuellas() {
        return desdeHex(parametros.obtener("documento.color_fondo_huellas", "#801A36"));
    }

    // ---- Firma y area que expide ----

    /** Quien firma los documentos. */
    public String firmanteNombre() {
        return parametros.obtener("documento.firmante_nombre",
                "Abog. Michele Eugenia Islas Ganime");
    }

    /** Cargo de quien firma. */
    public String firmanteCargo() {
        return parametros.obtener("documento.firmante_cargo",
                "Directora de Bienestar Animal del Sistema Estatal DIF Puebla");
    }

    /** Siglas del area que aparecen en los documentos. */
    public String areaSiglas() {
        return parametros.obtener("documento.area_siglas", "DIBA");
    }

    /** Nombre del area que expide. */
    public String areaNombre() {
        return parametros.obtener("documento.area_nombre",
                "Direccion de Bienestar Animal");
    }

    /** Ciudad que acompana la fecha de expedicion. */
    public String ciudadExpedicion() {
        return parametros.obtener("documento.ciudad_expedicion", "Puebla, Pue.");
    }

    /** Texto de la marca de agua del reverso. */
    public String marcaAguaTexto() {
        return parametros.obtener("documento.marca_agua_texto",
                "Certificado de empadronamiento");
    }

    /** Leyenda al pie que explica como verificar el documento. */
    public String leyendaVerificacion() {
        return parametros.obtener("certificado.leyenda_verificacion",
                "Verifica la autenticidad de este documento escaneando el codigo QR.");
    }

    /** Leyenda mientras la firma electronica avanzada no este disponible. */
    public String leyendaSelloFisico() {
        return parametros.obtener("certificado.leyenda_sello_fisico",
                "Si requieres el sello institucional, acude a las oficinas del "
                        + "Instituto de Bienestar Animal.");
    }

    /** Vigencia del certificado en meses; cero significa sin vencimiento. */
    public int vigenciaMeses() {
        return parametros.obtenerEntero("certificado.vigencia_meses", 12);
    }

    /**
     * Credenciales que caben en una hoja.
     *
     * <p>Son tres, y no es un numero arbitrario: asi las imprime hoy el personal
     * en opalina de 125 gramos, y cambiarlo desperdiciaria material.
     */
    public int credencialesPorHoja() {
        return parametros.obtenerEntero("certificado.credenciales_por_hoja", 3);
    }

    /**
     * Convierte un color de los ajustes.
     *
     * <p>Si el valor esta mal escrito se usa el guinda institucional en lugar de
     * fallar: un documento con el color equivocado se puede corregir; uno que no
     * se genera deja a alguien sin su tramite.
     */
    private Color desdeHex(String hex) {
        try {
            String limpio = hex.trim().replace("#", "");
            return new Color(
                    Integer.valueOf(limpio.substring(0, 2), 16),
                    Integer.valueOf(limpio.substring(2, 4), 16),
                    Integer.valueOf(limpio.substring(4, 6), 16));
        } catch (RuntimeException ex) {
            return new Color(0x80, 0x1A, 0x36);
        }
    }
}
