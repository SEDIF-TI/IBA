package mx.gob.sedif.sea.util.enums;

/** Forma en que se hace llegar el aviso a la persona. */
public enum CanalNotificacion {

    /** El sistema lo envia solo, por correo. */
    AUTOMATICO,

    /**
     * Lo tiene que hacer una persona: llamada o mensaje de WhatsApp.
     *
     * <p>Ocurre cuando el solicitante solo dejo telefono. Estas notificaciones
     * alimentan la bandeja de contacto manual; sin ellas, esos avisos
     * simplemente no ocurririan y nadie se enteraria de que hay gente
     * esperando respuesta.
     */
    MANUAL
}
