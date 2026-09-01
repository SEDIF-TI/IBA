-- ============================================================================
-- V16: Ajustes de los documentos que expide el instituto.
--
-- Los formatos oficiales que entrego el IBA usan una paleta rosa que todavia
-- no esta definida al cien por ciento. Por eso los colores del documento son
-- ajustes y no valores del codigo: cuando la institucion decida, se editan
-- desde el panel sin recompilar.
--
-- Los valores por omision son la paleta guinda vigente del sistema.
-- ============================================================================

INSERT INTO parametro (s_clave, s_valor, s_descripcion, s_tipo_dato, s_grupo, b_sistema) VALUES

    -- ---- Firma que autoriza los documentos ----
    -- Va como ajuste y no impreso en la plantilla: cuando cambie la titular se
    -- edita aqui, sin tocar el codigo ni volver a desplegar.
    ('documento.firmante_nombre', 'Abog. Michele Eugenia Islas Ganime',
     'Nombre de quien firma el certificado y la credencial',
     'TEXTO', 'documento', FALSE),
    ('documento.firmante_cargo',
     'Directora de Bienestar Animal del Sistema Estatal DIF Puebla',
     'Cargo de quien firma los documentos', 'TEXTO', 'documento', FALSE),

    -- ---- Identidad de los documentos ----
    ('documento.area_siglas', 'DIBA',
     'Siglas del area que aparecen en los documentos', 'TEXTO', 'documento', FALSE),
    ('documento.area_nombre', 'Direccion de Bienestar Animal',
     'Nombre del area que expide', 'TEXTO', 'documento', FALSE),
    ('documento.ciudad_expedicion', 'Puebla, Pue.',
     'Ciudad que aparece en la fecha de expedicion', 'TEXTO', 'documento', FALSE),

    -- ---- Colores del certificado y la credencial ----
    -- Pendientes de definicion. Por omision, el guinda institucional vigente.
    ('documento.color_titulo', '#801A36',
     'Color del titulo del certificado y la credencial',
     'TEXTO', 'documento', FALSE),
    ('documento.color_etiquetas', '#801A36',
     'Color de las etiquetas de los campos', 'TEXTO', 'documento', FALSE),
    ('documento.color_franja', '#801A36',
     'Color de la franja institucional superior', 'TEXTO', 'documento', FALSE),
    ('documento.color_cinta', '#64748B',
     'Color de la cinta con el nombre del padron', 'TEXTO', 'documento', FALSE),
    ('documento.color_fondo_huellas', '#801A36',
     'Color del fondo decorativo de huellas', 'TEXTO', 'documento', FALSE),

    -- ---- Leyendas del reverso ----
    ('documento.marca_agua_texto', 'Certificado de empadronamiento',
     'Texto de la marca de agua del reverso del certificado',
     'TEXTO', 'documento', FALSE);
