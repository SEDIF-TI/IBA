package mx.gob.sedif.sea.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Emite y verifica los tokens de sesion.
 *
 * <p>Se manejan dos tokens con proposito distinto:
 * <ul>
 *   <li><b>Acceso</b>: vida corta (una hora), viaja en la cabecera
 *       Authorization y lleva los roles. Al ser breve, un token robado sirve
 *       poco tiempo.</li>
 *   <li><b>Refresco</b>: vida larga (siete dias), viaja solo en una cookie
 *       HttpOnly para que JavaScript no pueda leerlo, y NO lleva roles: si a
 *       alguien se le retira un permiso, el siguiente refresco lo recoge de la
 *       base de datos.</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Distingue un token de refresco de uno de acceso. */
    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESO = "acceso";
    private static final String TIPO_REFRESCO = "refresco";

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USUARIO_ID = "uid";
    /** Versión de tokens de la cuenta ({@code usuario.n_token_version}) al emitirse el token. */
    private static final String CLAIM_VERSION = "ver";
    /** Sesión a la que pertenece un token de refresco ({@code sesion_usuario.s_identificador}). */
    private static final String CLAIM_SESION = "sid";

    private final SecretKey clave;
    private final long expiracionAcceso;
    private final long expiracionRefresco;
    private final String emisor;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.access-expiration}") long expiracionAcceso,
            @Value("${app.jwt.refresh-expiration}") long expiracionRefresco,
            @Value("${app.jwt.issuer}") String emisor) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionAcceso = expiracionAcceso;
        this.expiracionRefresco = expiracionRefresco;
        this.emisor = emisor;
    }

    /**
     * Emite el token de acceso, con el identificador del usuario, sus roles, su versión de tokens y
     * la sesión a la que pertenece.
     *
     * @param usuario principal ya autenticado
     * @param sesion  identificador de la sesión en {@code sesion_usuario}; cerrar esa sesión
     *                invalida también este token
     * @return token firmado
     */
    public String generarTokenAcceso(UsuarioAutenticado usuario, UUID sesion) {
        Date ahora = new Date();
        List<String> roles = usuario.getAuthorities().stream()
                .map(autoridad -> autoridad.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_VERSION, usuario.getTokenVersion())
                .claim(CLAIM_SESION, sesion.toString())
                .claim(CLAIM_TIPO, TIPO_ACCESO)
                .issuer(emisor)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionAcceso))
                .signWith(clave)
                .compact();
    }

    /**
     * Emite el token de refresco de una sesión.
     *
     * <p>Deliberadamente SIN roles: los permisos se releen de la base al refrescar, para que
     * revocar un permiso surta efecto sin esperar siete días. Lleva la sesión ({@code sid}) y un
     * identificador de un solo uso ({@code jti}) que {@code SesionService} rota en cada renovación.
     *
     * @param usuario principal ya autenticado
     * @param sesion  identificador de la sesión en {@code sesion_usuario}
     * @param jti     identificador de un solo uso vigente de esa sesión
     * @return token firmado
     */
    public String generarTokenRefresco(UsuarioAutenticado usuario, UUID sesion, String jti) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .id(jti)
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_VERSION, usuario.getTokenVersion())
                .claim(CLAIM_SESION, sesion.toString())
                .claim(CLAIM_TIPO, TIPO_REFRESCO)
                .issuer(emisor)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionRefresco))
                .signWith(clave)
                .compact();
    }

    /**
     * Verifica el token y devuelve su contenido en una sola lectura.
     *
     * @param token token recibido
     * @return los datos del token si la firma, el emisor y la vigencia son correctos; vacío si no
     */
    public Optional<DatosToken> leer(String token) {
        if (!esValido(token)) {
            return Optional.empty();
        }
        Claims claims = leerClaims(token);
        Object uid = claims.get(CLAIM_USUARIO_ID);
        Object version = claims.get(CLAIM_VERSION);
        String sesion = claims.get(CLAIM_SESION, String.class);
        try {
            return Optional.of(new DatosToken(
                    claims.get(CLAIM_TIPO, String.class),
                    claims.getSubject(),
                    uid == null ? null : Long.valueOf(uid.toString()),
                    version == null ? null : Integer.valueOf(version.toString()),
                    sesion == null ? null : UUID.fromString(sesion),
                    claims.getId()));
        } catch (IllegalArgumentException ex) {
            log.debug("Token con datos mal formados: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Comprueba firma, emisor y vigencia.
     *
     * @param token token a revisar
     * @return verdadero si el token es utilizable
     */
    public boolean esValido(String token) {
        try {
            leerClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Token expirado: {}", ex.getMessage());
        } catch (SignatureException ex) {
            // Firma que no cuadra: o el secreto cambio, o alguien fabrico el token.
            log.warn("Token con firma invalida");
        } catch (MalformedJwtException | UnsupportedJwtException ex) {
            log.debug("Token mal formado: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token no valido: {}", ex.getMessage());
        }
        return false;
    }

    /** Nombre de usuario contenido en el token. */
    public String obtenerUsuario(String token) {
        return leerClaims(token).getSubject();
    }

    /** Identificador del usuario contenido en el token. */
    public Long obtenerUsuarioId(String token) {
        Object valor = leerClaims(token).get(CLAIM_USUARIO_ID);
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    /** Verdadero si el token es de refresco y no de acceso. */
    public boolean esTokenRefresco(String token) {
        return TIPO_REFRESCO.equals(leerClaims(token).get(CLAIM_TIPO, String.class));
    }

    /** Verdadero si el token es de acceso. */
    public boolean esTokenAcceso(String token) {
        return TIPO_ACCESO.equals(leerClaims(token).get(CLAIM_TIPO, String.class));
    }

    /** Vigencia configurada del token de refresco, en segundos. */
    public long getExpiracionRefrescoSegundos() {
        return expiracionRefresco / 1000;
    }

    /** Vigencia configurada del token de acceso, en milisegundos. */
    public long getExpiracionAcceso() {
        return expiracionAcceso;
    }

    /**
     * Lee y verifica el contenido del token.
     *
     * <p>Exige que el emisor coincida: un token firmado con la misma clave por
     * otro sistema no se acepta aqui.
     */
    private Claims leerClaims(String token) {
        // El parser acepta el token solo si la firma corresponde a la clave del sistema.
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Contenido verificado de un token.
     *
     * @param tipo      {@code acceso} o {@code refresco}
     * @param usuario   nombre de usuario (subject)
     * @param usuarioId identificador de la cuenta; nulo en tokens anteriores a este formato
     * @param version   versión de tokens de la cuenta al emitirse; nula en tokens antiguos
     * @param sesion    sesión a la que pertenece el token; nula en tokens antiguos
     * @param jti       identificador de un solo uso del token de refresco; nulo en los de acceso
     */
    public record DatosToken(String tipo, String usuario, Long usuarioId, Integer version,
                             UUID sesion, String jti) {

        /** Verdadero si es un token de acceso. */
        public boolean esAcceso() {
            return TIPO_ACCESO.equals(tipo);
        }

        /** Verdadero si es un token de refresco. */
        public boolean esRefresco() {
            return TIPO_REFRESCO.equals(tipo);
        }
    }
}
