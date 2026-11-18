package mx.gob.sedif.sea.core.auditoria;

/**
 * Acciones que quedan registradas en la bitacora.
 *
 * <p>Se enumeran para que la consulta de bitacora pueda filtrar por valores
 * conocidos, en lugar de depender de textos escritos a mano que acabarian
 * teniendo variantes.
 */
public final class AccionAuditoria {

    private AccionAuditoria() {
    }

    // ---- Resolucion de tramites ----
    public static final String APROBAR = "APROBAR";
    public static final String OBSERVAR = "OBSERVAR";
    public static final String RECHAZAR = "RECHAZAR";
    public static final String CANCELAR = "CANCELAR";

    // ---- Datos personales: lo que mas importa vigilar ----
    /** Consulta del padron por CURP. */
    public static final String CONSULTAR_CURP = "CONSULTAR_CURP";
    /** Descarga de un INE, comprobante o cualquier documento de la persona. */
    public static final String DESCARGAR_DOCUMENTO = "DESCARGAR_DOCUMENTO";
    /** Visualizacion de la ficha completa de una persona. */
    public static final String CONSULTAR_PERSONA = "CONSULTAR_PERSONA";

    // ---- Documentos ----
    public static final String SUBIR_DOCUMENTO = "SUBIR_DOCUMENTO";
    public static final String REVISAR_DOCUMENTO = "REVISAR_DOCUMENTO";
    public static final String ELIMINAR_DOCUMENTO = "ELIMINAR_DOCUMENTO";

    // ---- Certificados ----
    public static final String EMITIR_CERTIFICADO = "EMITIR_CERTIFICADO";
    public static final String CANCELAR_CERTIFICADO = "CANCELAR_CERTIFICADO";

    // ---- Mantenimiento ----
    public static final String CREAR = "CREAR";
    public static final String MODIFICAR = "MODIFICAR";
    public static final String ELIMINAR = "ELIMINAR";

    // ---- Acceso ----
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String LOGOUT = "LOGOUT";
    public static final String CAMBIO_PASSWORD = "CAMBIO_PASSWORD";
    public static final String CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";
    /** Sesión cerrada por el sistema: token de renovación reutilizado o copiado. */
    public static final String SESION_REVOCADA = "SESION_REVOCADA";
}
