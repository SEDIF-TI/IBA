package mx.gob.sedif.sea.util.enums;

/** Situacion en que se encuentra un aviso. */
public enum EstatusEnvio {

    /** Creada y en espera: por enviar, o por llamar si el canal es manual. */
    PENDIENTE,

    /** Correo entregado al servidor de salida. */
    ENVIADA,

    /** El envio automatico fallo; admite reintento. */
    FALLIDA,

    /** Contacto manual logrado. */
    CONTACTADA,

    /** Se intento el contacto manual sin exito. */
    NO_CONTACTADA
}
