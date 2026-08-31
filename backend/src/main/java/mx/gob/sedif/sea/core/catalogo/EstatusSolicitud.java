package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Estado del tramite dentro de la maquina de estados.
 *
 * <p>Sustituye las banderas magicas del legado: un campo {@code activo} con
 * valores 1, 0 y 3 sin documentar, y un {@code estado} que siempre valia
 * "Proceso".
 *
 * <p>Las transiciones validas se aplican en la capa de servicio; aqui solo
 * viven las propiedades de cada estado.
 */
@Entity
@Table(name = "cat_estatus_solicitud")
public class EstatusSolicitud extends CatalogoBase {

    /** Estado final: no admite mas transiciones. */
    @Column(name = "b_terminal", nullable = false)
    private Boolean terminal = Boolean.FALSE;

    /** Exige motivo del catalogo al entrar. Aplica a OBSERVADA y RECHAZADA. */
    @Column(name = "b_requiere_motivo", nullable = false)
    private Boolean requiereMotivo = Boolean.FALSE;

    /** Color de la insignia en el panel, tomado de la paleta institucional. */
    @Column(name = "s_color_ui", length = 20)
    private String colorUi;

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public Boolean getRequiereMotivo() {
        return requiereMotivo;
    }

    public void setRequiereMotivo(Boolean requiereMotivo) {
        this.requiereMotivo = requiereMotivo;
    }

    public String getColorUi() {
        return colorUi;
    }

    public void setColorUi(String colorUi) {
        this.colorUi = colorUi;
    }
}
