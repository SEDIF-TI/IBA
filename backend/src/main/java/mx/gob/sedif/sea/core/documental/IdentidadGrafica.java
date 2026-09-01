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
