package mx.gob.sedif.sea.util.enums;

import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.APROBADA;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.CANCELADA;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.CERTIFICADO_EMITIDO;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.ENTREGADA;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.EN_REVISION;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.OBSERVADA;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.RECHAZADA;
import static mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.REGISTRADA;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Verifica las reglas de la maquina de estados del tramite. */
class EstatusSolicitudEnumTest {

    @Nested
    @DisplayName("Camino normal del tramite")
    class CaminoFeliz {

        @Test
        @DisplayName("recorre registrada hasta entregada")
        void recorreElFlujoCompleto() {
            assertThat(REGISTRADA.puedeTransicionarA(EN_REVISION)).isTrue();
            assertThat(EN_REVISION.puedeTransicionarA(APROBADA)).isTrue();
            assertThat(APROBADA.puedeTransicionarA(CERTIFICADO_EMITIDO)).isTrue();
            assertThat(CERTIFICADO_EMITIDO.puedeTransicionarA(ENTREGADA)).isTrue();
        }
    }

    @Nested
    @DisplayName("Correccion de una solicitud observada")
    class Correccion {

        @Test
        @DisplayName("observada regresa a revision cuando la persona corrige")
        void observadaVuelveARevision() {
            assertThat(EN_REVISION.puedeTransicionarA(OBSERVADA)).isTrue();
            assertThat(OBSERVADA.puedeTransicionarA(EN_REVISION)).isTrue();
        }

        @Test
        @DisplayName("una observada no salta directo a aprobada")
        void observadaNoSaltaAAprobada() {
            assertThat(OBSERVADA.puedeTransicionarA(APROBADA)).isFalse();
        }
    }

    @Nested
    @DisplayName("Movimientos prohibidos")
    class Prohibidas {

        @Test
        @DisplayName("no se aprueba sin haber pasado por revision")
        void noSeApruebaDirecto() {
            assertThat(REGISTRADA.puedeTransicionarA(APROBADA)).isFalse();
            assertThat(REGISTRADA.puedeTransicionarA(CERTIFICADO_EMITIDO)).isFalse();
        }

        @Test
        @DisplayName("no se emite certificado sin aprobar antes")
        void noEmiteSinAprobar() {
            assertThat(EN_REVISION.puedeTransicionarA(CERTIFICADO_EMITIDO)).isFalse();
        }

        @Test
        @DisplayName("un estado no transiciona a si mismo")
        void noTransicionaASiMismo() {
            for (EstatusSolicitudEnum estatus : EstatusSolicitudEnum.values()) {
                assertThat(estatus.puedeTransicionarA(estatus))
                        .as("%s no debe transicionar a si mismo", estatus)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("un estado terminal ya no se mueve")
        void terminalesNoSeMueven() {
            for (EstatusSolicitudEnum terminal : new EstatusSolicitudEnum[]{ENTREGADA, RECHAZADA, CANCELADA}) {
                for (EstatusSolicitudEnum destino : EstatusSolicitudEnum.values()) {
                    assertThat(terminal.puedeTransicionarA(destino))
                            .as("%s es terminal y no debe moverse a %s", terminal, destino)
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("un tramite entregado no se puede rechazar despues")
        void entregadaNoSeRechaza() {
            assertThat(ENTREGADA.puedeTransicionarA(RECHAZADA)).isFalse();
            assertThat(ENTREGADA.puedeTransicionarA(CANCELADA)).isFalse();
        }
    }

    @Nested
    @DisplayName("Cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("se admite desde cualquier estado vivo")
        void seCancelaDesdeEstadosVivos() {
            for (EstatusSolicitudEnum estatus : EstatusSolicitudEnum.values()) {
                if (!estatus.esTerminal()) {
                    assertThat(estatus.puedeTransicionarA(CANCELADA))
                            .as("%s deberia poder cancelarse", estatus)
                            .isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("Propiedades de cada estado")
    class Propiedades {

        @Test
        @DisplayName("los terminales son entregada, rechazada y cancelada")
        void identificaTerminales() {
            assertThat(ENTREGADA.esTerminal()).isTrue();
            assertThat(RECHAZADA.esTerminal()).isTrue();
            assertThat(CANCELADA.esTerminal()).isTrue();
            assertThat(REGISTRADA.esTerminal()).isFalse();
            assertThat(EN_REVISION.esTerminal()).isFalse();
            assertThat(APROBADA.esTerminal()).isFalse();
        }

        @Test
        @DisplayName("observar, rechazar y cancelar exigen motivo del catalogo")
        void exigenMotivo() {
            assertThat(OBSERVADA.exigeMotivo()).isTrue();
            assertThat(RECHAZADA.exigeMotivo()).isTrue();
            assertThat(CANCELADA.exigeMotivo()).isTrue();
            assertThat(APROBADA.exigeMotivo()).isFalse();
        }

        @Test
        @DisplayName("desde revision se ofrece aprobar, observar, rechazar y cancelar")
        void enumeraSiguientes() {
            assertThat(EN_REVISION.siguientesPosibles())
                    .containsExactlyInAnyOrder(APROBADA, OBSERVADA, RECHAZADA, CANCELADA);
        }

        @Test
        @DisplayName("un estado terminal no ofrece siguientes")
        void terminalSinSiguientes() {
            assertThat(ENTREGADA.siguientesPosibles()).isEmpty();
        }
    }
}
