package mx.gob.sedif.sea.core.certificado;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

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

/**
 * Certificado de empadronamiento emitido al aprobar la solicitud.
 */
@Entity
@Table(name = "certificado")
public class Certificado extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pn_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_solicitud_id", nullable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_certificado_solicitud"))
    private Solicitud solicitud;

    /** Folio oficial definitivo, distinto del folio de la solicitud. */
    @NotBlank
    @Column(name = "s_folio", nullable = false, length = 80, updatable = false)
    private String folio;

    /**
     * Codigo que viaja en el QR. Es opaco a proposito: quien lo escanea puede
     * comprobar que el certificado es autentico, pero no obtiene ningun dato
     * personal del tutor.
     */
    @Column(name = "s_codigo_verificacion", nullable = false, updatable = false)
    private UUID codigoVerificacion;

    @Column(name = "d_fecha_emision", nullable = false, updatable = false)
    private OffsetDateTime fechaEmision = OffsetDateTime.now();

    /** Nula si la institucion decide que el certificado no vence. */
    @Column(name = "d_fecha_vigencia")
    private LocalDate fechaVigencia;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fn_usuario_emisor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_certificado_usuario_emisor"))
    private Usuario usuarioEmisor;

    /**
     * Huella del PDF emitido. Permite comprobar que un documento presentado
     * coincide exactamente con el que expidio el sistema.
     */
    @Column(name = "s_hash_documento", length = 64)
    private String hashDocumento;

    /**
     * Firma electronica avanzada. Sigue pendiente de resolucion juridica, asi
     * que estas columnas quedan listas y el servicio de firma es enchufable.
     */
    @Column(name = "b_firmado_electronicamente", nullable = false)
    private Boolean firmadoElectronicamente = Boolean.FALSE;

    @Column(name = "d_fecha_firma")
    private OffsetDateTime fechaFirma;

    @Column(name = "s_sello_firma")
    private String selloFirma;

    /** Mientras no haya firma avanzada, el sello institucional va en fisico. */
    @Column(name = "b_sello_fisico_entregado", nullable = false)
    private Boolean selloFisicoEntregado = Boolean.FALSE;

    @Column(name = "d_fecha_entrega")
    private OffsetDateTime fechaEntrega;

    @Column(name = "b_vigente", nullable = false)
    private Boolean vigente = Boolean.TRUE;

    @Column(name = "s_motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "d_fecha_cancelacion")
    private OffsetDateTime fechaCancelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fn_usuario_cancelacion_id",
                foreignKey = @ForeignKey(name = "fk_certificado_usuario_cancelacion"))
    private Usuario usuarioCancelacion;

    /** Cancela el certificado dejando constancia de quien y por que. */
    public void cancelar(String motivo, Usuario responsable) {
        this.vigente = Boolean.FALSE;
        this.motivoCancelacion = motivo;
        this.fechaCancelacion = OffsetDateTime.now();
        this.usuarioCancelacion = responsable;
    }

    /** Registra la entrega fisica del documento sellado. */
    public void registrarEntrega() {
        this.selloFisicoEntregado = Boolean.TRUE;
        this.fechaEntrega = OffsetDateTime.now();
    }

    /** Verdadero si el certificado sigue surtiendo efecto hoy. */
    public boolean estaVigente() {
        if (Boolean.FALSE.equals(vigente)) {
            return false;
        }
        return fechaVigencia == null || !fechaVigencia.isBefore(LocalDate.now());
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

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public UUID getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public void setCodigoVerificacion(UUID codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }

    public OffsetDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(OffsetDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaVigencia() {
        return fechaVigencia;
    }

    public void setFechaVigencia(LocalDate fechaVigencia) {
        this.fechaVigencia = fechaVigencia;
    }

    public Usuario getUsuarioEmisor() {
        return usuarioEmisor;
    }

    public void setUsuarioEmisor(Usuario usuarioEmisor) {
        this.usuarioEmisor = usuarioEmisor;
    }

    public String getHashDocumento() {
        return hashDocumento;
    }

    public void setHashDocumento(String hashDocumento) {
        this.hashDocumento = hashDocumento;
    }

    public Boolean getFirmadoElectronicamente() {
        return firmadoElectronicamente;
    }

    public void setFirmadoElectronicamente(Boolean firmadoElectronicamente) {
        this.firmadoElectronicamente = firmadoElectronicamente;
    }

    public OffsetDateTime getFechaFirma() {
        return fechaFirma;
    }

    public void setFechaFirma(OffsetDateTime fechaFirma) {
        this.fechaFirma = fechaFirma;
    }

    public String getSelloFirma() {
        return selloFirma;
    }

    public void setSelloFirma(String selloFirma) {
        this.selloFirma = selloFirma;
    }

    public Boolean getSelloFisicoEntregado() {
        return selloFisicoEntregado;
    }

    public void setSelloFisicoEntregado(Boolean selloFisicoEntregado) {
        this.selloFisicoEntregado = selloFisicoEntregado;
    }

    public OffsetDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(OffsetDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Boolean getVigente() {
        return vigente;
    }

    public void setVigente(Boolean vigente) {
        this.vigente = vigente;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public OffsetDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(OffsetDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public Usuario getUsuarioCancelacion() {
        return usuarioCancelacion;
    }

    public void setUsuarioCancelacion(Usuario usuarioCancelacion) {
        this.usuarioCancelacion = usuarioCancelacion;
    }
}
