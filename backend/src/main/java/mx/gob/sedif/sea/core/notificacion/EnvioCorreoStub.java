package mx.gob.sedif.sea.core.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implementacion vigente mientras no haya servidor de correo.
 *
 * <p>No envia nada y lo hace constar. Deliberadamente no finge un envio
 * correcto: si lo hiciera, el sistema marcaria como avisadas a personas que
 * nunca recibieron nada, y nadie se enteraria hasta que alguien reclamara.
 *
 * <p>Al devolver un fallo explicito, esos avisos quedan pendientes y visibles
 * para que el personal los atienda por telefono.
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false",
                       matchIfMissing = true)
public class EnvioCorreoStub implements EnvioCorreoService {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoStub.class);

    public EnvioCorreoStub() {
        log.warn("""

                ============================================================
                EL ENVIO DE CORREO ESTA DESHABILITADO

                Los avisos NO se enviaran. Quedaran registrados y visibles en
                la bandeja de contacto para atenderlos por telefono.

                Para habilitarlo, configura en el archivo .env:
                    APP_MAIL_ENABLED=true
                    APP_MAIL_HOST, APP_MAIL_PORT, APP_MAIL_USERNAME,
                    APP_MAIL_PASSWORD y APP_MAIL_FROM
                ============================================================""");
    }

    @Override
    public ResultadoEnvio enviar(String destinatario, String asunto, String cuerpo) {
        log.info("Correo no enviado (envio deshabilitado). Destinatario: {}, asunto: {}",
                destinatario, asunto);

        return ResultadoEnvio.fallo(
                "El envio de correo no esta configurado en el sistema, asi que este "
                        + "aviso no salio. Comunicate por telefono con la persona.");
    }

    @Override
    public boolean estaHabilitado() {
        return false;
    }
}
