package mx.gob.sedif.sea.core.animal;

import java.time.LocalDate;

import mx.gob.sedif.sea.core.catalogo.Color;
import mx.gob.sedif.sea.core.catalogo.Especie;
import mx.gob.sedif.sea.core.catalogo.MetodoIdentificacion;
import mx.gob.sedif.sea.core.catalogo.Raza;
import mx.gob.sedif.sea.core.catalogo.Sexo;
import mx.gob.sedif.sea.core.catalogo.Tamano;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Ser sintiente empadronado: perro o gato.
 *
 * <p>El vinculo con su tutor NO esta aqui, sino en {@code titularidad}, que
 * lleva vigencia. Un animal se transfiere, se adopta o el tutor fallece; con
 * una llave foranea directa se perderia esa historia.
 */
@Entity
@Table(name = "animal")
public class Animal extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotBlank
    @Column(name = "s_nombre", nullable = false, length = 120)
    private String nombre;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_especie_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_animal_cat_especie"))
    private Especie especie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_raza_id",
                foreignKey = @ForeignKey(name = "fk_animal_cat_raza"))
    private Raza raza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_color_id",
                foreignKey = @ForeignKey(name = "fk_animal_cat_color"))
    private Color color;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_sexo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_animal_cat_sexo"))
    private Sexo sexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_tamano_id",
                foreignKey = @ForeignKey(name = "fk_animal_cat_tamano"))
    private Tamano tamano;

    /**
     * Edad normalizada en meses. El legado la guardaba como texto libre: "1",
     * "1 ANO", "2 MESES". Asi no se puede ordenar, filtrar ni sacar
     * estadistica. La interfaz captura anos y meses y convierte antes de
     * guardar.
     */
    @Min(0)
    @Max(600)
    @Column(name = "n_edad_meses")
    private Integer edadMeses;

    @Column(name = "d_fecha_nacimiento_aproximada")
    private LocalDate fechaNacimientoAproximada;

    /** Booleano de verdad, no la cadena "SI" que usaba el legado. */
    @Column(name = "b_vacunado", nullable = false)
    private Boolean vacunado = Boolean.FALSE;

    @Column(name = "b_esterilizado", nullable = false)
    private Boolean esterilizado = Boolean.FALSE;

    /** Senas particulares que ayudan a identificarlo. */
    @Column(name = "s_caracteristicas", length = 1000)
    private String caracteristicas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_metodo_identificacion_id",
                foreignKey = @ForeignKey(name = "fk_animal_cat_metodo_identificacion"))
    private MetodoIdentificacion metodoIdentificacion;

    /** Numero de microchip, placa o tatuaje. */
    @Column(name = "s_identificador", length = 60)
    private String identificador;

    /**
     * Identificador interno del animal, con patron configurable.
     *
     * <p>El legado lo llamaba "CURP de mascota". No se usa ese nombre: confunde
     * con la CURP real de la persona y no tiene nada que ver con RENAPO.
     */
    @Column(name = "s_clave_animal", length = 60)
    private String claveAnimal;

    @Column(name = "b_finado", nullable = false)
    private Boolean finado = Boolean.FALSE;

    @Column(name = "d_fecha_defuncion")
    private LocalDate fechaDefuncion;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /** Edad legible para documentos, a partir de los meses normalizados. */
    public String getEdadDescriptiva() {
        if (edadMeses == null) {
            return null;
        }
        int anios = edadMeses / 12;
        int meses = edadMeses % 12;
        if (anios == 0) {
            return meses + (meses == 1 ? " mes" : " meses");
        }
        String textoAnios = anios + (anios == 1 ? " ano" : " anos");
        if (meses == 0) {
            return textoAnios;
        }
        return textoAnios + " " + meses + (meses == 1 ? " mes" : " meses");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Especie getEspecie() {
        return especie;
    }

    public void setEspecie(Especie especie) {
        this.especie = especie;
    }

    public Raza getRaza() {
        return raza;
    }

    public void setRaza(Raza raza) {
        this.raza = raza;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public Tamano getTamano() {
        return tamano;
    }

    public void setTamano(Tamano tamano) {
        this.tamano = tamano;
    }

    public Integer getEdadMeses() {
        return edadMeses;
    }

    public void setEdadMeses(Integer edadMeses) {
        this.edadMeses = edadMeses;
    }

    public LocalDate getFechaNacimientoAproximada() {
        return fechaNacimientoAproximada;
    }

    public void setFechaNacimientoAproximada(LocalDate fechaNacimientoAproximada) {
        this.fechaNacimientoAproximada = fechaNacimientoAproximada;
    }

    public Boolean getVacunado() {
        return vacunado;
    }

    public void setVacunado(Boolean vacunado) {
        this.vacunado = vacunado;
    }

    public Boolean getEsterilizado() {
        return esterilizado;
    }

    public void setEsterilizado(Boolean esterilizado) {
        this.esterilizado = esterilizado;
    }

    public String getCaracteristicas() {
        return caracteristicas;
    }

    public void setCaracteristicas(String caracteristicas) {
        this.caracteristicas = caracteristicas;
    }

    public MetodoIdentificacion getMetodoIdentificacion() {
        return metodoIdentificacion;
    }

    public void setMetodoIdentificacion(MetodoIdentificacion metodoIdentificacion) {
        this.metodoIdentificacion = metodoIdentificacion;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getClaveAnimal() {
        return claveAnimal;
    }

    public void setClaveAnimal(String claveAnimal) {
        this.claveAnimal = claveAnimal;
    }

    public Boolean getFinado() {
        return finado;
    }

    public void setFinado(Boolean finado) {
        this.finado = finado;
    }

    public LocalDate getFechaDefuncion() {
        return fechaDefuncion;
    }

    public void setFechaDefuncion(LocalDate fechaDefuncion) {
        this.fechaDefuncion = fechaDefuncion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
