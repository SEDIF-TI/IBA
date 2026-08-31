package mx.gob.sedif.sea.security.auth;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Lleva la cuenta de los intentos de acceso fallidos.
 *
 * <p>Vive en su propia clase por un motivo concreto: el incremento debe
 * ejecutarse en una transaccion independiente, y Spring solo aplica esa regla
 * cuando la llamada cruza de un componente a otro. Si estuviera dentro de
 * AuthService, la invocacion no pasaria por el proxy, la anotacion se ignoraria
 * en silencio y el contador se perderia al revertirse la transaccion de la
 * autenticacion fallida.
 *
 * <p>Ese detalle no es cosmetico: sin el, la cuenta admitiria intentos
 * ilimitados y el bloqueo por fuerza bruta nunca se activaria.
 */
@Service
public class IntentosAccesoService {

    private static final Logger log = LoggerFactory.getLogger(IntentosAccesoService.class);

    private final UsuarioRepository usuarioRepository;
    private final int maxIntentosFallidos;
    private final int minutosBloqueo;

    public IntentosAccesoService(
            UsuarioRepository usuarioRepository,
            @Value("${app.seguridad.max-intentos-fallidos:5}") int maxIntentosFallidos,
            @Value("${app.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.usuarioRepository = usuarioRepository;
        this.maxIntentosFallidos = maxIntentosFallidos;
        this.minutosBloqueo = minutosBloqueo;
    }

    /**
     * Suma un intento fallido y bloquea la cuenta al alcanzar el limite.
     *
     * <p>La transaccion es nueva e independiente para que sobreviva al
     * descarte de la transaccion que la invoca.
     *
     * <p>El bloqueo es temporal: pasado el plazo la cuenta se libera sola. Uno
     * permanente permitiria dejar fuera a cualquiera a proposito, con solo
     * fallar cinco veces contra su nombre de usuario.
     *
     * @param usuarioId identificador de la cuenta que fallo
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return;
        }

        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        if (intentos >= maxIntentosFallidos) {
            usuario.setBloqueoHasta(OffsetDateTime.now().plusMinutes(minutosBloqueo));
            log.warn("Cuenta '{}' bloqueada {} minutos tras {} intentos fallidos",
                    usuario.getUsuario(), minutosBloqueo, intentos);
        } else {
            log.info("Intento fallido {} de {} para '{}'",
                    intentos, maxIntentosFallidos, usuario.getUsuario());
        }

        usuarioRepository.saveAndFlush(usuario);
    }

    /** Limpia el contador tras un acceso correcto. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccesoCorrecto(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueoHasta(null);
            usuario.setUltimoAcceso(OffsetDateTime.now());
            usuarioRepository.saveAndFlush(usuario);
        });
    }
}
