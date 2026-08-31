package mx.gob.sedif.sea.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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
     * Emite el token de acceso, con el identificador del usuario y sus roles.
     *
     * @param usuario principal ya autenticado
     * @return token firmado
     */
    public String generarTokenAcceso(UsuarioAutenticado usuario) {
        Date ahora = new Date();
        List<String> roles = usuario.getAuthorities().stream()
                .map(autoridad -> autoridad.getAuthority())
                .toList();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TIPO, TIPO_ACCESO)
                .issuer(emisor)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionAcceso))
                .signWith(clave)
                .compact();
    }

    /**
     * Emite el token de refresco.
     *
     * <p>Deliberadamente SIN roles: los permisos se releen de la base al
     * refrescar, para que revocar un permiso surta efecto sin esperar siete
     * dias.
     *
     * @param usuario principal ya autenticado
     * @return token firmado
     */
    public String generarTokenRefresco(UsuarioAutenticado usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_TIPO, TIPO_REFRESCO)
                .issuer(emisor)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionRefresco))
                .signWith(clave)
                .compact();
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
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(emisor)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
