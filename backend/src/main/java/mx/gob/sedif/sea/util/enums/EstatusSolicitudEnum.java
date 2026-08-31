package mx.gob.sedif.sea.util.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del tramite y transiciones permitidas entre ellos.
 *
 * <p>Los estados viven en la tabla {@code cat_estatus_solicitud} para que el
 * personal pueda editar su nombre, color y descripcion. Este enumerado no los
 * duplica: fija las CLAVES y, sobre todo, que movimiento es legal, que es una
 * regla de negocio y no un dato administrable.
 *
 * <pre>
 * REGISTRADA -&gt; EN_REVISION -&gt; APROBADA -&gt; CERTIFICADO_EMITIDO -&gt; ENTREGADA
 *                    |
 *                    +-&gt; OBSERVADA -&gt; EN_REVISION   (correccion por liga)
 *                    |
 *                    +-&gt; RECHAZADA
 *
 * Desde cualquier estado no terminal: -&gt; CANCELADA
 * </pre>
 *
 * <p>Sustituye las banderas magicas del legado: un campo {@code activo} con
 * valores 1, 0 y 3 sin documentar, y un {@code estado} que siempre valia
 * "Proceso".
 */
public enum EstatusSolicitudEnum {

    /** Recibida por el portal, la jornada o la ventanilla. Aun sin revisar. */
    REGISTRADA,

    /** El personal esta cotejando datos contra documentos. */
    EN_REVISION,

    /** Requiere correccion de la persona; se le envia liga con token. */
    OBSERVADA,

    /** Procede el empadronamiento. */
    APROBADA,

    /** El documento ya se genero y espera entrega. */
    CERTIFICADO_EMITIDO,

    /** La persona recogio su documentacion. Tramite concluido. */
    ENTREGADA,

    /** No procede. Exige motivo del catalogo. */
    RECHAZADA,

    /** Cancelada antes de concluir. Exige motivo. */
    CANCELADA;

    /** Estados que ya no admiten ningun movimiento. */
    private static final Set<EstatusSolicitudEnum> TERMINALES =
            Collections.unmodifiableSet(EnumSet.of(ENTREGADA, RECHAZADA, CANCELADA));

    /** Estados que exigen motivo del catalogo al entrar en ellos. */
    private static final Set<EstatusSolicitudEnum> EXIGEN_MOTIVO =
            Collections.unmodifiableSet(EnumSet.of(OBSERVADA, RECHAZADA, CANCELADA));

    /**
     * Movimientos permitidos desde cada estado.
     *
     * <p>CANCELADA no aparece en las listas porque se admite desde cualquier
     * estado no terminal; eso lo resuelve {@link #puedeTransicionarA}.
     */
    private static final Map<EstatusSolicitudEnum, Set<EstatusSolicitudEnum>> PERMITIDAS = Map.of(
            REGISTRADA,          EnumSet.of(EN_REVISION),
            EN_REVISION,         EnumSet.of(APROBADA, OBSERVADA, RECHAZADA),
            OBSERVADA,           EnumSet.of(EN_REVISION),
            APROBADA,            EnumSet.of(CERTIFICADO_EMITIDO),
            CERTIFICADO_EMITIDO, EnumSet.of(ENTREGADA),
            ENTREGADA,           EnumSet.noneOf(EstatusSolicitudEnum.class),
            RECHAZADA,           EnumSet.noneOf(EstatusSolicitudEnum.class),
            CANCELADA,           EnumSet.noneOf(EstatusSolicitudEnum.class));

    /** Verdadero si el tramite ya llego a su fin. */
    public boolean esTerminal() {
        return TERMINALES.contains(this);
    }

    /** Verdadero si entrar a este estado obliga a indicar un motivo del catalogo. */
    public boolean exigeMotivo() {
        return EXIGEN_MOTIVO.contains(this);
    }

    /**
     * Indica si es legal moverse de este estado al indicado.
     *
     * @param destino estado al que se quiere pasar
     * @return verdadero si la transicion esta permitida
     */
    public boolean puedeTransicionarA(EstatusSolicitudEnum destino) {
        if (destino == null || destino == this) {
            return false;
        }
        // La cancelacion se admite mientras el tramite siga vivo.
        if (destino == CANCELADA) {
            return !esTerminal();
        }
        return PERMITIDAS.getOrDefault(this, Set.of()).contains(destino);
    }

    /** Estados a los que se puede pasar desde este. */
    public Set<EstatusSolicitudEnum> siguientesPosibles() {
        Set<EstatusSolicitudEnum> siguientes = EnumSet.copyOf(PERMITIDAS.getOrDefault(this, Set.of()));
        if (!esTerminal()) {
            siguientes.add(CANCELADA);
        }
        return Collections.unmodifiableSet(siguientes);
    }
}
