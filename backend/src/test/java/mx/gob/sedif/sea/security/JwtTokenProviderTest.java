package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;

/** Verifica la emision y verificacion de los tokens de sesion. */
class JwtTokenProviderTest {

    /** Secretos generados en cada ejecución: ninguna clave queda escrita en el código. */
    private static final String SECRETO = secretoAleatorio();
    private static final String OTRO_SECRETO = secretoAleatorio();
    private static final UUID SESION = UUID.randomUUID();

    private static String secretoAleatorio() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    private static final String EMISOR = "sea-dif-puebla";

    private JwtTokenProvider proveedor;
    private UsuarioAutenticado usuario;

    @BeforeEach
    void prepararEscenario() {
        proveedor = new JwtTokenProvider(SECRETO, 3_600_000L, 604_800_000L, EMISOR);
        usuario = new UsuarioAutenticado(
                42L, "jperez", "$2a$10$hash", true, false, true, 3,
                Set.of("ADMINISTRADOR"),
                Set.of("solicitud:aprobar"));
    }

    @Nested
    @DisplayName("Token de acceso")
    class TokenDeAcceso {

        @Test
        @DisplayName("lleva la sesión y la versión de tokens, sin identificador de un solo uso")
        void llevaSesionYVersion() {
            JwtTokenProvider.DatosToken datos = proveedor
                    .leer(proveedor.generarTokenAcceso(usuario, SESION)).orElseThrow();

            assertThat(datos.esAcceso()).isTrue();
            assertThat(datos.sesion()).isEqualTo(SESION);
            assertThat(datos.version()).isEqualTo(3);
            assertThat(datos.jti()).isNull();
        }

        @Test
        @DisplayName("conserva el usuario, su identificador y sus roles")
        void llevaDatosDelUsuario() {
            String token = proveedor.generarTokenAcceso(usuario, SESION);

            assertThat(proveedor.esValido(token)).isTrue();
            assertThat(proveedor.obtenerUsuario(token)).isEqualTo("jperez");
            assertThat(proveedor.obtenerUsuarioId(token)).isEqualTo(42L);
            assertThat(proveedor.esTokenAcceso(token)).isTrue();
            assertThat(proveedor.esTokenRefresco(token)).isFalse();

            // El rol lleva prefijo; el permiso va tal cual.
            assertThat(leerRoles(token))
                    .containsExactlyInAnyOrder("ROLE_ADMINISTRADOR", "solicitud:aprobar");
        }
    }

    @Nested
    @DisplayName("Token de refresco")
    class TokenDeRefresco {

        @Test
        @DisplayName("lleva su sesión, su identificador de un solo uso y la versión de la cuenta")
        void llevaSesionYVersion() {
            JwtTokenProvider.DatosToken datos = proveedor
                    .leer(proveedor.generarTokenRefresco(usuario, SESION, "jti-1")).orElseThrow();

            assertThat(datos.esRefresco()).isTrue();
            assertThat(datos.sesion()).isEqualTo(SESION);
            assertThat(datos.jti()).isEqualTo("jti-1");
            assertThat(datos.version()).isEqualTo(3);
            assertThat(datos.usuarioId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("NO lleva roles, para que revocar un permiso surta efecto pronto")
        void noLlevaRoles() {
            String token = proveedor.generarTokenRefresco(usuario, SESION, "jti-1");

            assertThat(proveedor.esValido(token)).isTrue();
            assertThat(proveedor.esTokenRefresco(token)).isTrue();
            assertThat(leerRoles(token))
                    .as("el token de refresco no debe contener roles")
                    .isNull();
        }

        @Test
        @DisplayName("se distingue del token de acceso")
        void seDistingueDelDeAcceso() {
            String refresco = proveedor.generarTokenRefresco(usuario, SESION, "jti-1");
            assertThat(proveedor.esTokenAcceso(refresco)).isFalse();
        }
    }

    @Nested
    @DisplayName("Verificacion")
    class Verificacion {

        @Test
        @DisplayName("rechaza un token firmado con otra clave")
        void rechazaFirmaAjena() {
            JwtTokenProvider intruso =
                    new JwtTokenProvider(OTRO_SECRETO, 3_600_000L, 604_800_000L, EMISOR);
            String tokenAjeno = intruso.generarTokenAcceso(usuario, SESION);

            assertThat(proveedor.esValido(tokenAjeno))
                    .as("un token firmado con otra clave no debe aceptarse")
                    .isFalse();
        }

        @Test
        @DisplayName("rechaza un token manipulado")
        void rechazaTokenAlterado() {
            String token = proveedor.generarTokenAcceso(usuario, SESION);
            String alterado = token.substring(0, token.length() - 4) + "AAAA";

            assertThat(proveedor.esValido(alterado)).isFalse();
        }

        @Test
        @DisplayName("rechaza un token ya expirado")
        void rechazaTokenExpirado() {
            JwtTokenProvider yaVencido =
                    new JwtTokenProvider(SECRETO, -1_000L, -1_000L, EMISOR);
            String token = yaVencido.generarTokenAcceso(usuario, SESION);

            assertThat(proveedor.esValido(token)).isFalse();
        }

        @Test
        @DisplayName("rechaza un token emitido por otro sistema con la misma clave")
        void rechazaOtroEmisor() {
            JwtTokenProvider otroSistema =
                    new JwtTokenProvider(SECRETO, 3_600_000L, 604_800_000L, "otro-sistema");
            String token = otroSistema.generarTokenAcceso(usuario, SESION);

            assertThat(proveedor.esValido(token))
                    .as("el emisor debe coincidir aunque la clave sea la misma")
                    .isFalse();
        }

        @Test
        @DisplayName("rechaza basura y cadenas vacias")
        void rechazaEntradaInvalida() {
            assertThat(proveedor.esValido("esto-no-es-un-token")).isFalse();
            assertThat(proveedor.esValido("")).isFalse();
            assertThat(proveedor.esValido("a.b.c")).isFalse();
        }
    }

    /** Lee los roles del token usando la clave directamente. */
    @SuppressWarnings("unchecked")
    private List<String> leerRoles(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", List.class);
    }
}
