package mx.gob.sedif.sea.util;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * Columnas de auditoria que lleva toda tabla transaccional del sistema.
 *
 * <p>Spring Data las llena solo: las fechas con el reloj del servidor y los
 * usuarios con el {@code AuditorAware} configurado en
 * {@link mx.gob.sedif.sea.config.AuditoriaConfig}.
 *
 * <p>Los identificadores de usuario quedan nulos cuando la accion viene del
 * portal publico. Es lo esperado, no un error: el registro ciudadano es
 * anonimo, la ciudadania no tiene cuenta en el sistema.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "d_fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @LastModifiedDate
    @Column(name = "d_fecha_modificacion")
    private OffsetDateTime fechaModificacion;

    @CreatedBy
    @Column(name = "fn_usuario_creacion_id", updatable = false)
    private Long usuarioCreacionId;

    @LastModifiedBy
    @Column(name = "fn_usuario_modificacion_id")
    private Long usuarioModificacionId;

    /**
     * Versión para el bloqueo optimista ({@code n_version}). Hibernate la incrementa en cada
     * actualización y rechaza el cambio si otra persona guardó antes; los formularios de edición
     * mandan la versión que tenían a la vista para detectar también ese caso.
     */
    @Version
    @Column(name = "n_version", nullable = false)
    private Long version;

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(OffsetDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public Long getUsuarioCreacionId() {
        return usuarioCreacionId;
    }

    public void setUsuarioCreacionId(Long usuarioCreacionId) {
        this.usuarioCreacionId = usuarioCreacionId;
    }

    public Long getUsuarioModificacionId() {
        return usuarioModificacionId;
    }

    public void setUsuarioModificacionId(Long usuarioModificacionId) {
        this.usuarioModificacionId = usuarioModificacionId;
    }
}
