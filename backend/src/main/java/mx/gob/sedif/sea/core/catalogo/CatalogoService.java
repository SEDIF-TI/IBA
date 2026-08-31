package mx.gob.sedif.sea.core.catalogo;

import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

/**
 * Operaciones comunes a todos los catalogos administrables.
 *
 * <p>Los trece catalogos se comportan igual, asi que la logica se escribe una
 * vez y cada uno la reutiliza indicando su repositorio y su nombre. La
 * alternativa serian trece servicios casi identicos y trece sitios donde
 * corregir el mismo detalle.
 *
 * @param <T> entidad de catalogo
 */
public class CatalogoService<T extends CatalogoBase> {

    private static final Logger log = LoggerFactory.getLogger(CatalogoService.class);

    private final CatalogoRepository<T> repositorio;
    private final Supplier<T> constructor;
    private final String nombreEntidad;
    private final AuditoriaService auditoria;

    /**
     * @param repositorio   acceso a datos del catalogo
     * @param constructor   crea una instancia vacia de la entidad
     * @param nombreEntidad nombre de la tabla, para bitacora y mensajes
     * @param auditoria     registro de cambios
     */
    public CatalogoService(CatalogoRepository<T> repositorio,
                           Supplier<T> constructor,
                           String nombreEntidad,
                           AuditoriaService auditoria) {
        this.repositorio = repositorio;
        this.constructor = constructor;
        this.nombreEntidad = nombreEntidad;
        this.auditoria = auditoria;
    }

    /**
     * Listado paginado para el panel.
     *
     * @param texto       filtro sobre clave y nombre; nulo para no filtrar
     * @param soloActivos excluye los dados de baja
     */
    @Transactional(readOnly = true)
    public Page<T> buscar(String texto, boolean soloActivos, Pageable pageable) {
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return repositorio.buscar(filtro, soloActivos, pageable);
    }

    /**
     * Opciones vigentes, en el orden definido.
     *
     * <p>Es lo que alimenta los desplegables de los formularios.
     */
    @Transactional(readOnly = true)
    public List<T> listarVigentes() {
        return repositorio.findByActivoTrueOrderByOrdenAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public T obtener(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(nombreEntidad, id));
    }

    /** Busca por la clave estable que usa el codigo. */
    @Transactional(readOnly = true)
    public T obtenerPorClave(String clave) {
        return repositorio.findByClave(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException(nombreEntidad, clave));
    }

    /**
     * Da de alta un valor nuevo.
     *
     * @throws ReglaNegocioException si la clave ya esta en uso
     */
    @Transactional
    public T crear(CatalogoRequest peticion) {
        String clave = peticion.clave().trim().toUpperCase();

        if (repositorio.existsByClave(clave)) {
            throw new ReglaNegocioException(
                    "Ya existe un registro con la clave '%s'".formatted(clave));
        }

        T entidad = constructor.get();
        entidad.setClave(clave);
        aplicar(entidad, peticion);

        T guardado = repositorio.save(entidad);

        auditoria.registrar(AccionAuditoria.CREAR, nombreEntidad, null,
                "Alta en %s: %s".formatted(nombreEntidad, guardado.getNombre()));

        log.info("Alta en catalogo {}: {}", nombreEntidad, clave);
        return guardado;
    }

    /**
     * Modifica un valor existente.
     *
     * <p>La clave no se cambia: el codigo la usa como referencia estable y
     * cambiarla romperia lo que dependa de ella. Para eso se da de baja y se
     * crea otra.
     */
    @Transactional
    public T actualizar(Integer id, CatalogoRequest peticion) {
        T entidad = obtener(id);
        String nombreAnterior = entidad.getNombre();

        aplicar(entidad, peticion);
        T guardado = repositorio.save(entidad);

        auditoria.registrar(AccionAuditoria.MODIFICAR, nombreEntidad, id.longValue(),
                "Cambio en %s: '%s' pasa a '%s'".formatted(
                        nombreEntidad, nombreAnterior, guardado.getNombre()));

        return guardado;
    }

    /**
     * Da de baja un valor.
     *
     * <p>La baja es logica: los registros historicos siguen apuntando a este
     * valor, y borrarlo de verdad los dejaria sin referencia. Deja de ofrecerse
     * en los formularios, pero lo ya capturado se sigue leyendo.
     */
    @Transactional
    public void desactivar(Integer id) {
        T entidad = obtener(id);

        if (Boolean.FALSE.equals(entidad.getActivo())) {
            throw new ReglaNegocioException("El registro ya estaba dado de baja");
        }

        entidad.setActivo(false);
        repositorio.save(entidad);

        auditoria.registrar(AccionAuditoria.ELIMINAR, nombreEntidad, id.longValue(),
                "Baja en %s: %s".formatted(nombreEntidad, entidad.getNombre()));

        log.info("Baja en catalogo {}: {}", nombreEntidad, entidad.getClave());
    }

    /** Vuelve a poner disponible un valor dado de baja. */
    @Transactional
    public T reactivar(Integer id) {
        T entidad = obtener(id);
        entidad.setActivo(true);
        T guardado = repositorio.save(entidad);

        auditoria.registrar(AccionAuditoria.MODIFICAR, nombreEntidad, id.longValue(),
                "Reactivacion en %s: %s".formatted(nombreEntidad, entidad.getNombre()));

        return guardado;
    }

    /** Copia los datos editables de la peticion a la entidad. */
    private void aplicar(T entidad, CatalogoRequest peticion) {
        entidad.setNombre(peticion.nombre().trim());
        entidad.setDescripcion(peticion.descripcion() == null
                ? null : peticion.descripcion().trim());
        if (peticion.orden() != null) {
            entidad.setOrden(peticion.orden());
        }
        if (peticion.activo() != null) {
            entidad.setActivo(peticion.activo());
        }
    }

    /** Nombre de la tabla que administra este servicio. */
    protected String getNombreEntidad() {
        return nombreEntidad;
    }

    /** Repositorio subyacente, para los catalogos con consultas propias. */
    protected CatalogoRepository<T> getRepositorio() {
        return repositorio;
    }
}
