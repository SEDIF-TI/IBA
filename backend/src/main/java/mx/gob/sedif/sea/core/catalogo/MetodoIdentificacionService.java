package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de formas de identificar al animal. */
@Service
public class MetodoIdentificacionService extends CatalogoService<MetodoIdentificacion> {

    public MetodoIdentificacionService(MetodoIdentificacionRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, MetodoIdentificacion::new, "cat_metodo_identificacion", auditoria);
    }
}
