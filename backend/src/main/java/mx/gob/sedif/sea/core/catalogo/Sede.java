package mx.gob.sedif.sea.core.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Punto donde la ciudadania recoge su documentacion.
 *
 * <p>La direccion de entrega vive aqui, administrable, y no codificada en las
 * plantillas de correo: cuando el IBA cambia de domicilio se edita un registro,
 * no se recompila el sistema.
 */
@Entity
@Table(name = "cat_sede")
public class Sede extends CatalogoBase {

    @NotBlank
    @Column(name = "s_domicilio", nullable = false, length = 400)
    private String domicilio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_municipio_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cat_sede_cat_municipio"))
    private Municipio municipio;

    @Column(name = "s_horario", length = 200)
    private String horario;

    @Column(name = "s_telefono", length = 30)
    private String telefono;

    @Column(name = "s_correo", length = 150)
    private String correo;

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
