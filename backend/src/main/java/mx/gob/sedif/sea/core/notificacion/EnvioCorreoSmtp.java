package mx.gob.sedif.sea.core.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio real de correo por SMTP.
 *
 * <p>Solo se activa cuando {@code app.mail.enabled} vale verdadero y hay un
 * servidor configurado.
 *
 * <p>Los fallos se traducen a una causa concreta y no a un "no se pudo enviar":
 * quien revise la bandeja necesita saber si el problema fue la direccion, las
 * credenciales del servidor o la red, porque cada uno se resuelve distinto.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EnvioCorreoSmtp implements EnvioCorreoService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoSmtp.class);

    private final JavaMailSender remitente;
    private final String direccionOrigen;

    public EnvioCorreoSmtp(JavaMailSender remitente,
                           @Value("${app.mail.from}") String direccionOrigen) {
        this.remitente = remitente;
        this.direccionOrigen = direccionOrigen;
        log.info("Envio de correo habilitado; los avisos salen desde {}", direccionOrigen);
    }

    @Override
    public ResultadoEnvio enviar(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(direccionOrigen);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            remitente.send(mensaje);

            log.info("Correo enviado a {}", enmascarar(destinatario));
            return ResultadoEnvio.exito();

        } catch (MailAuthenticationException ex) {
            // El servidor rechazo las credenciales: es un problema de
            // configuracion, no de la direccion de destino.
            log.error("El servidor de correo rechazo las credenciales", ex);
            return ResultadoEnvio.fallo(
                    "El servidor de correo rechazo las credenciales del sistema. "
                            + "Revisa la configuración con informática.");

        } catch (MailSendException ex) {
            // Suele ser una direccion inexistente o un buzon lleno.
            log.warn("No se pudo entregar el correo a {}: {}",
                    enmascarar(destinatario), ex.getMessage());
            return ResultadoEnvio.fallo(
                    "El servidor no aceptó la dirección '%s'. Verifica que esté bien "
                            .formatted(destinatario)
                            + "escrita o comunícate por teléfono.");

        } catch (MailException ex) {
            log.error("Fallo el envio de correo a {}", enmascarar(destinatario), ex);
            return ResultadoEnvio.fallo(
                    "No se pudo conectar con el servidor de correo. "
                            + "Se reintentara automaticamente.");
        }
    }

    @Override
    public boolean estaHabilitado() {
        return true;
    }

    /** Deja la direccion irreconocible en la bitacora del servidor. */
    private String enmascarar(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "(sin dirección)";
        }
        String[] partes = correo.split("@", 2);
        String usuario = partes[0];
        String visible = usuario.length() <= 2
                ? usuario.charAt(0) + "*"
                : usuario.substring(0, 2) + "*".repeat(usuario.length() - 2);
        return visible + "@" + partes[1];
    }
}
