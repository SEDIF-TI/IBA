package mx.gob.sedif.sea.core.catalogo;

import org.springframework.stereotype.Service;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;

/** Administracion de tipos de documento. */
@Service
public class TipoDocumentoService extends CatalogoService<TipoDocumento> {

    public TipoDocumentoService(TipoDocumentoRepository repositorio, AuditoriaService auditoria) {
        super(repositorio, TipoDocumento::new, "cat_tipo_documento", auditoria);
    }
}
