package mx.gob.sedif.sea.security;

import java.util.HashSet;
import java.util.Set;

import mx.gob.sedif.sea.core.catalogo.CatalogoBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Rol asignable al personal administrativo.
 *
 * <p>Arrancamos con dos: SUPERADMINISTRADOR para informatica y ADMINISTRADOR
 * para el IBA. Estan en tabla y no en un enum de Java para poder agregar
 * roles desde el panel sin recompilar.
 */
@Entity
@Table(name = "cat_rol")
public class Rol extends CatalogoBase {

    /**
     * Rol protegido: no se borra ni se renombra desde el panel. Evita que
     * alguien elimine SUPERADMINISTRADOR y deje el sistema sin quien lo
     * administre.
     */
    @Column(name = "b_sistema", nullable = false)
    private Boolean sistema = Boolean.FALSE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rol_permiso",
            joinColumns = @JoinColumn(name = "fn_rol_id",
                    foreignKey = @ForeignKey(name = "fk_rol_permiso_cat_rol")),
            inverseJoinColumns = @JoinColumn(name = "fn_permiso_id",
                    foreignKey = @ForeignKey(name = "fk_rol_permiso_cat_permiso")))
    private Set<Permiso> permisos = new HashSet<>();

    public Boolean getSistema() {
        return sistema;
    }

    public void setSistema(Boolean sistema) {
        this.sistema = sistema;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }

    public void setPermisos(Set<Permiso> permisos) {
        this.permisos = permisos;
    }
}
