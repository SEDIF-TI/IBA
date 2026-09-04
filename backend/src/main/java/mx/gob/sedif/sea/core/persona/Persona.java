package mx.gob.sedif.sea.core.persona;

import java.time.LocalDate;

import mx.gob.sedif.sea.core.catalogo.MedioContacto;
import mx.gob.sedif.sea.core.catalogo.Municipio;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Tutor responsable del animal.
 *
 * <p>No es una cuenta de acceso: la ciudadania no se autentica. Es el registro
 * de quien responde por el animal.
 *
 * <p>La identidad es la CURP, pero deliberadamente NO es llave primaria ni
 * foranea. RENAPO emite correcciones de CURP; si fuera llave, corregir una
 * habria que propagarla por todo el modelo.
 *
 * <p>La CURP tampoco es obligatoria. Si la persona no la trae se registra con
 * su nombre completo y queda con {@code identidadValidada} en falso; el
 * personal la completa despues a partir del INE.
 */
@Entity
@Table(name = "persona")
public class Persona extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    /**
     * Unica cuando no es nula, garantizado por un indice unico parcial.
     * El dígito verificador se valida en la capa de servicio: la regla de
     * RENAPO es un algoritmo, no una expresion regular.
     */
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[0-9A-Z][0-9]$",
             message = "La CURP no tiene el formato válido")
    @Column(name = "s_curp", length = 18)
    private String curp;

    /** Falso mientras la CURP no se coteje contra el documento oficial. */
    @Column(name = "b_identidad_validada", nullable = false)
    private Boolean identidadValidada = Boolean.FALSE;

    /**
     * El nombre va partido en tres columnas. En el legado estaba en una sola y
     * eso hacia imposible buscar por apellidos, que es justo como se localiza a
     * la gente el dia de una jornada municipal.
     */
    @NotBlank
    @Size(max = 120)
    @Column(name = "s_nombre", nullable = false, length = 120)
    private String nombre;

    @NotBlank
    @Size(max = 120)
    @Column(name = "s_apellido_paterno", nullable = false, length = 120)
    private String apellidoPaterno;

    @Size(max = 120)
    @Column(name = "s_apellido_materno", length = 120)
    private String apellidoMaterno;

    @Email
    @Column(name = "s_correo", length = 150)
    private String correo;

    @Column(name = "s_telefono", length = 20)
    private String telefono;

    @Column(name = "s_telefono_whatsapp", length = 20)
    private String telefonoWhatsapp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_medio_contacto_preferido_id",
                foreignKey = @ForeignKey(name = "fk_persona_cat_medio_contacto"))
    private MedioContacto medioContactoPreferido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_municipio_id",
                foreignKey = @ForeignKey(name = "fk_persona_cat_municipio"))
    private Municipio municipio;

    @Column(name = "s_calle", length = 200)
    private String calle;

    @Column(name = "s_numero_exterior", length = 20)
    private String numeroExterior;

    @Column(name = "s_numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "s_colonia", length = 150)
    private String colonia;

    @Pattern(regexp = "^[0-9]{5}$", message = "El codigo postal debe tener 5 dígitos")
    @Column(name = "s_codigo_postal", length = 5)
    private String codigoPostal;

    /** Se deriva de la CURP cuando esta disponible. */
    @Column(name = "d_fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** Derivado de la CURP; sirve para cotejar contra lo capturado. */
    @Column(name = "s_sexo_curp", length = 1)
    private String sexoCurp;

    /** Clave de la entidad de nacimiento, derivada de la CURP. */
    @Column(name = "s_entidad_nacimiento", length = 2)
    private String entidadNacimiento;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /** Nombre completo, para pantallas y documentos. */
    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder(nombre).append(' ').append(apellidoPaterno);
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            sb.append(' ').append(apellidoMaterno);
        }
        return sb.toString();
    }

    /**
     * CURP enmascarada para listados: deja ver las cuatro primeras letras y las
     * dos ultimas posiciones. Nunca se muestra completa fuera de la ficha, y
     * jamas aparece en el verificador publico.
     */
    public String getCurpEnmascarada() {
        if (curp == null || curp.length() != 18) {
            return null;
        }
        return curp.substring(0, 4) + "**********" + curp.substring(16);
    }

    /** Verdadero si dejo al menos una via de contacto, que es obligatorio. */
    public boolean tieneContacto() {
        return (correo != null && !correo.isBlank())
                || (telefono != null && !telefono.isBlank())
                || (telefonoWhatsapp != null && !telefonoWhatsapp.isBlank());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public Boolean getIdentidadValidada() {
        return identidadValidada;
    }

    public void setIdentidadValidada(Boolean identidadValidada) {
        this.identidadValidada = identidadValidada;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTelefonoWhatsapp() {
        return telefonoWhatsapp;
    }

    public void setTelefonoWhatsapp(String telefonoWhatsapp) {
        this.telefonoWhatsapp = telefonoWhatsapp;
    }

    public MedioContacto getMedioContactoPreferido() {
        return medioContactoPreferido;
    }

    public void setMedioContactoPreferido(MedioContacto medioContactoPreferido) {
        this.medioContactoPreferido = medioContactoPreferido;
    }

    public Municipio getMunicipio() {
        return municipio;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getNumeroExterior() {
        return numeroExterior;
    }

    public void setNumeroExterior(String numeroExterior) {
        this.numeroExterior = numeroExterior;
    }

    public String getNumeroInterior() {
        return numeroInterior;
    }

    public void setNumeroInterior(String numeroInterior) {
        this.numeroInterior = numeroInterior;
    }

    public String getColonia() {
        return colonia;
    }

    public void setColonia(String colonia) {
        this.colonia = colonia;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getSexoCurp() {
        return sexoCurp;
    }

    public void setSexoCurp(String sexoCurp) {
        this.sexoCurp = sexoCurp;
    }

    public String getEntidadNacimiento() {
        return entidadNacimiento;
    }

    public void setEntidadNacimiento(String entidadNacimiento) {
        this.entidadNacimiento = entidadNacimiento;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
