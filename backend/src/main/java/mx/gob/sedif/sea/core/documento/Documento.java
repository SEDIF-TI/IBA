package mx.gob.sedif.sea.core.documento;

import java.time.OffsetDateTime;

import mx.gob.sedif.sea.core.catalogo.TipoDocumento;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.security.Usuario;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Metadatos de un archivo adjunto a la solicitud.
 *
 * <p>El archivo en si NO esta aqui ni bajo ningun directorio publicado: vive en
 * un volumen fuera del alcance web, con nombre generado, y solo se entrega por
 * un endpoint autenticado que valida rol y deja registro en bitacora.
 *
 * <p>En el legado estos documentos se guardaban bajo el directorio publico con
 * direccion predecible: cualquiera podia descargar el INE de un tercero
 * adivinando la URL. Es la falla mas grave que heredamos y no se repite.
 */
@Entity
@Table(name = "documento")
public class Documento extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_solicitud_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_documento_solicitud"))
    private Solicitud solicitud;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_tipo_documento_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_documento_cat_tipo"))
    private TipoDocumento tipoDocumento;

    /**
     * Nombre con el que la persona subio el archivo. Solo se usa para
     * mostrarlo: jamas para construir la ruta en disco, porque un nombre
     * malicioso podria escaparse del directorio previsto.
     */
    @NotBlank
    @Column(name = "s_nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    /** Nombre real en disco: identificador generado que no revela nada. */
    @NotBlank
    @Column(name = "s_nombre_almacenado", nullable = false, length = 150, updatable = false)
    private String nombreAlmacenado;

    /** Ruta relativa dentro del volumen protegido. Nunca una direccion publica. */
    @NotBlank
    @Column(name = "s_ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    /**
     * Tipo detectado por el contenido del archivo, no por su extension:
     * renombrar un ejecutable a .jpg no debe bastar para colarlo.
     */
    @NotBlank
    @Column(name = "s_mime", nullable = false, length = 120)
    private String mime;

    @Positive
    @Column(name = "n_tamano_bytes", nullable = false)
    private Long tamanoBytes;

    /** Huella del archivo: detecta duplicados y prueba que no fue alterado. */
    @NotBlank
    @Column(name = "s_hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    /**
     * Marca del validador. Nulo mientras nadie lo revisa.
     *
     * <p>Falso significa ilegible: hay que pedir reenvio. Es el problema mas
     * frecuente con el INE, y en el legado obligaba a perseguir a la persona
     * por WhatsApp.
     */
    @Column(name = "b_legible")
    private Boolean legible;

    @Column(name = "s_observacion_revision", length = 500)
    private String observacionRevision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_revision_id",
                foreignKey = @ForeignKey(name = "fk_documento_usuario_revision"))
    private Usuario usuarioRevision;

    @Column(name = "d_fecha_revision")
    private OffsetDateTime fechaRevision;

    @Column(name = "b_activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    /**
     * Registra el resultado de la revision, dejando constancia de quien la hizo
     * y cuando.
     */
    public void revisar(boolean esLegible, String observacion, Usuario revisor) {
        this.legible = esLegible;
        this.observacionRevision = observacion;
        this.usuarioRevision = revisor;
        this.fechaRevision = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getHashSha256() {
        return hashSha256;
    }

    public void setHashSha256(String hashSha256) {
        this.hashSha256 = hashSha256;
    }

    public Boolean getLegible() {
        return legible;
    }

    public void setLegible(Boolean legible) {
        this.legible = legible;
    }

    public String getObservacionRevision() {
        return observacionRevision;
    }

    public void setObservacionRevision(String observacionRevision) {
        this.observacionRevision = observacionRevision;
    }

    public Usuario getUsuarioRevision() {
        return usuarioRevision;
    }

    public void setUsuarioRevision(Usuario usuarioRevision) {
        this.usuarioRevision = usuarioRevision;
    }

    public OffsetDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(OffsetDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
