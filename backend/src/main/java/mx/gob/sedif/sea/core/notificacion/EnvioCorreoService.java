package mx.gob.sedif.sea.core.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio de correo.
 *
 * <p>Hay dos implementaciones y la que opera depende de {@code app.mail.enabled}:
 * esta, que envia de verdad, y {@link EnvioCorreoStub}, que solo registra.
 * Mientras el area no proporcione un servidor de correo, opera la segunda.
 */
public interface EnvioCorreoService {

    /**
     * Envia un correo.
     *
     * @param destinatario direccion de destino
     * @param asunto       asunto del mensaje
     * @param cuerpo       texto del mensaje
     * @return resultado del intento
     */
    ResultadoEnvio enviar(String destinatario, String asunto, String cuerpo);

    /** Indica si el envio real esta configurado. */
    boolean estaHabilitado();

    /**
     * Resultado de un intento de envio.
     *
     * @param enviado si el correo salio
     * @param detalle explicacion; en un fallo, la causa concreta
     */
    record ResultadoEnvio(boolean enviado, String detalle) {

        public static ResultadoEnvio exito() {
            return new ResultadoEnvio(true, "Correo entregado al servidor de salida");
        }

        public static ResultadoEnvio fallo(String causa) {
            return new ResultadoEnvio(false, causa);
        }
    }
}
