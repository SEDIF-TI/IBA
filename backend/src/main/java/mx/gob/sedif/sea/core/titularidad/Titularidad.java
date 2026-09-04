package mx.gob.sedif.sea.core.titularidad;

import java.time.LocalDate;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.util.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Vinculo entre una persona y un animal, con vigencia.
 *
 * <p>Deliberadamente no es una llave foranea de animal a persona. Un animal se
 * transfiere, se adopta, el tutor fallece o el animal muere; con llave directa
 * se pierde la historia y el conteo "cuantos animales tiene está persona" se
 * vuelve ambiguo.
 *
 * <p>Aqui ese conteo son las titularidades vigentes, y el historico se
 * conserva completo. Un indice unico parcial impide que un animal tenga dos
 * titularidades vigentes al mismo tiempo.
 */
@Entity
@Table(name = "titularidad")
public class Titularidad extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_persona_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_titularidad_persona"))
    private Persona persona;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_animal_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_titularidad_animal"))
    private Animal animal;

    @Column(name = "d_fecha_inicio", nullable = false)
    private LocalDate fechaInicio = LocalDate.now();

    /** Nula mientras la titularidad siga vigente. */
    @Column(name = "d_fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "s_motivo_baja", length = 300)
    private String motivoBaja;

    @Column(name = "b_vigente", nullable = false)
    private Boolean vigente = Boolean.TRUE;

    /**
     * Cierra la titularidad. Se usa al transferir el animal, al darlo de baja
     * por defuncion o cuando el tutor deja de responder por el.
     *
     * @param motivo razon de la baja, que queda registrada
     */
    public void cerrar(String motivo) {
        this.vigente = Boolean.FALSE;
        this.fechaFin = LocalDate.now();
        this.motivoBaja = motivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente;
    }
}
