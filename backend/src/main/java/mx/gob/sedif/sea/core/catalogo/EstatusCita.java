package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Estado de la cita de entrega: agendada, atendida, no asistio o reagendada.
 */
@Entity
@Table(name = "cat_estatus_cita")
public class EstatusCita extends CatalogoBase {

    @Column(name = "b_terminal", nullable = false)
    private Boolean terminal = Boolean.FALSE;

    @Column(name = "s_color_ui", length = 20)
    private String colorUi;

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public String getColorUi() {
        return colorUi;
    }

    public void setColorUi(String colorUi) {
        this.colorUi = colorUi;
    }
}
