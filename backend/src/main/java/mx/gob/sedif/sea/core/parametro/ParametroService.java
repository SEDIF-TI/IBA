package mx.gob.sedif.sea.core.parametro;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

/**
 * Lectura y edicion de los ajustes del sistema.
 *
 * <p>Aqui viven los patrones de folio, las vigencias, las leyendas de los
 * documentos y la identidad grafica. Se consultan en cada tramite, asi que se
 * guardan en memoria: cambian muy rara vez y no tiene sentido ir a la base cada
 * vez.
 */
@Service
public class ParametroService {

    private static final Logger log = LoggerFactory.getLogger(ParametroService.class);

    private final ParametroRepository parametroRepository;
    private final AuditoriaService auditoria;

    public ParametroService(ParametroRepository parametroRepository,
                            AuditoriaService auditoria) {
        this.parametroRepository = parametroRepository;
        this.auditoria = auditoria;
    }

    /**
     * Valor de un ajuste.
     *
     * @param clave        identificador del ajuste
     * @param porOmision   valor a devolver si no esta configurado
     */
    @Cacheable(value = "parametros", key = "#clave", unless = "#result == null")
    @Transactional(readOnly = true)
    public String obtener(String clave, String porOmision) {
        return parametroRepository.findByClaveAndActivoTrue(clave)
                .map(Parametro::getValor)
                .orElse(porOmision);
    }

    /**
     * Valor de un ajuste que el sistema necesita si o si.
     *
     * @throws RecursoNoEncontradoException si no esta configurado
     */
    @Transactional(readOnly = true)
    public String obtenerObligatorio(String clave) {
        return parametroRepository.findByClaveAndActivoTrue(clave)
                .map(Parametro::getValor)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No esta configurado el parametro '%s'".formatted(clave)));
    }

    /** Valor numerico de un ajuste. */
    @Transactional(readOnly = true)
    public int obtenerEntero(String clave, int porOmision) {
        String valor = obtener(clave, null);
        if (valor == null || valor.isBlank()) {
            return porOmision;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            log.warn("El parametro '{}' vale '{}', que no es un numero; se usa {}",
                    clave, valor, porOmision);
            return porOmision;
        }
    }

    /** Valor de si o no de un ajuste. */
    @Transactional(readOnly = true)
    public boolean obtenerBooleano(String clave, boolean porOmision) {
        String valor = obtener(clave, null);
        return (valor == null || valor.isBlank())
                ? porOmision
                : Boolean.parseBoolean(valor.trim());
    }

    @Transactional(readOnly = true)
    public List<Parametro> listarTodos() {
        return parametroRepository.findByActivoTrueOrderByGrupoAscClaveAsc();
    }

    @Transactional(readOnly = true)
    public List<Parametro> listarPorGrupo(String grupo) {
        return parametroRepository.findByGrupoAndActivoTrueOrderByClave(grupo);
    }

    /**
     * Cambia el valor de un ajuste.
     *
     * <p>Vacia la memoria intermedia para que el cambio surta efecto de
     * inmediato en vez de esperar a que caduque.
     */
    @CacheEvict(value = "parametros", allEntries = true)
    @Transactional
    public Parametro actualizar(String clave, String valorNuevo) {
        Parametro parametro = parametroRepository.findByClaveAndActivoTrue(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("Parametro", clave));

        String valorAnterior = parametro.getValor();
        validarSegunTipo(parametro, valorNuevo);

        parametro.setValor(valorNuevo);
        Parametro guardado = parametroRepository.save(parametro);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "parametro",
                parametro.getId().longValue(),
                "El parametro '%s' pasa de '%s' a '%s'".formatted(
                        clave, valorAnterior, valorNuevo));

        log.info("Parametro '{}' actualizado", clave);
        return guardado;
    }

    /** Comprueba que el valor corresponda con el tipo declarado. */
    private void validarSegunTipo(Parametro parametro, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El valor no puede quedar vacio");
        }

        switch (parametro.getTipoDato()) {
            case "ENTERO" -> {
                try {
                    Integer.parseInt(valor.trim());
                } catch (NumberFormatException ex) {
                    throw new ReglaNegocioException(
                            "El parametro '%s' espera un numero entero"
                                    .formatted(parametro.getClave()));
                }
            }
            case "DECIMAL" -> {
                try {
                    Double.parseDouble(valor.trim());
                } catch (NumberFormatException ex) {
                    throw new ReglaNegocioException(
                            "El parametro '%s' espera un numero"
                                    .formatted(parametro.getClave()));
                }
            }
            case "BOOLEANO" -> {
                String limpio = valor.trim().toLowerCase();
                if (!limpio.equals("true") && !limpio.equals("false")) {
                    throw new ReglaNegocioException(
                            "El parametro '%s' solo admite true o false"
                                    .formatted(parametro.getClave()));
                }
            }
            default -> {
                // TEXTO, FECHA y JSON no se comprueban aqui.
            }
        }
    }
}
