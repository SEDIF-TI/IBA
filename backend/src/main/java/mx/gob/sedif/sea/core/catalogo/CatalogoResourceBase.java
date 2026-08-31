package mx.gob.sedif.sea.core.catalogo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import mx.gob.sedif.sea.exception.ApiResponse;
import mx.gob.sedif.sea.exception.MessageConstants;

import jakarta.validation.Valid;

/**
 * Endpoints comunes de un catalogo administrable.
 *
 * <p>Cada catalogo hereda de aqui y solo declara su ruta y su servicio. Los
 * trece se comportan igual, y escribirlos por separado significaria repetir
 * seis metodos identicos trece veces, con trece sitios donde olvidar un
 * permiso.
 *
 * <p>La consulta exige el permiso de ver catalogos; toda modificacion exige el
 * de administrarlos.
 *
 * @param <T> entidad de catalogo
 */
public abstract class CatalogoResourceBase<T extends CatalogoBase> {

    /** Servicio que atiende este catalogo. */
    protected abstract CatalogoService<T> getServicio();

    /**
     * Convierte la entidad al registro que se devuelve.
     *
     * <p>Los catalogos con campos propios sobrescriben esto para incluirlos.
     */
    protected Object aRecord(T entidad) {
        return CatalogoRecord.de(entidad);
    }

    /** Listado paginado para la pantalla de administracion. */
    @GetMapping
    @PreAuthorize("hasAuthority('catalogo:ver')")
    public ResponseEntity<ApiResponse<Page<Object>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "false") boolean soloActivos,
            @PageableDefault(size = 20, sort = "orden", direction = Sort.Direction.ASC)
            Pageable pageable) {

        Page<Object> pagina = getServicio()
                .buscar(texto, soloActivos, pageable)
                .map(this::aRecord);

        return ResponseEntity.ok(ApiResponse.ok(pagina));
    }

    /**
     * Opciones vigentes para los desplegables de los formularios.
     *
     * <p>Sin paginar: un desplegable necesita la lista completa.
     */
    @GetMapping("/opciones")
    @PreAuthorize("hasAuthority('catalogo:ver')")
    public ResponseEntity<ApiResponse<List<Object>>> opciones() {
        List<Object> opciones = getServicio().listarVigentes().stream()
                .map(this::aRecord)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(opciones));
    }

    /** Un registro concreto. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('catalogo:ver')")
    public ResponseEntity<ApiResponse<Object>> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(aRecord(getServicio().obtener(id))));
    }

    /** Alta de un valor nuevo. */
    @PostMapping
    @PreAuthorize("hasAuthority('catalogo:administrar')")
    public ResponseEntity<ApiResponse<Object>> crear(
            @Valid @RequestBody CatalogoRequest peticion) {

        T creado = getServicio().crear(peticion);
        return ResponseEntity.ok(
                ApiResponse.ok(aRecord(creado), MessageConstants.REGISTRO_CREADO));
    }

    /** Modificacion de un valor existente. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('catalogo:administrar')")
    public ResponseEntity<ApiResponse<Object>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody CatalogoRequest peticion) {

        T actualizado = getServicio().actualizar(id, peticion);
        return ResponseEntity.ok(
                ApiResponse.ok(aRecord(actualizado), MessageConstants.REGISTRO_ACTUALIZADO));
    }

    /** Baja logica: deja de ofrecerse, pero lo capturado sigue siendo legible. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('catalogo:administrar')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Integer id) {
        getServicio().desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok(MessageConstants.REGISTRO_ELIMINADO));
    }

    /** Vuelve a poner disponible un valor dado de baja. */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('catalogo:administrar')")
    public ResponseEntity<ApiResponse<Object>> reactivar(@PathVariable Integer id) {
        T reactivado = getServicio().reactivar(id);
        return ResponseEntity.ok(
                ApiResponse.ok(aRecord(reactivado), "Registro reactivado correctamente"));
    }
}
