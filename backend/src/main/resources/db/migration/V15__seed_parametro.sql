-- ============================================================================
-- V15: Parametros del sistema, plantillas de aviso y contenido publico.
--
-- Todo lo que la institucion puede querer cambiar vive aqui, no en el codigo:
-- patrones de folio, vigencias, leyendas, identidad grafica y datos de
-- contacto. El sistema pasa de SMADSOT a DIF y nada de eso deberia obligar a
-- recompilar.
-- ============================================================================

INSERT INTO parametro (s_clave, s_valor, s_descripcion, s_tipo_dato, s_grupo, b_sistema) VALUES
    -- ---- Identidad institucional ----
    ('institucion.nombre', 'Sistema Estatal DIF Puebla',
     'Nombre de la institucion que expide', 'TEXTO', 'institucion', TRUE),
    ('institucion.area', 'Instituto de Bienestar Animal',
     'Area responsable del padron', 'TEXTO', 'institucion', TRUE),
    ('institucion.siglas', 'IBA',
     'Siglas del area responsable', 'TEXTO', 'institucion', TRUE),
    ('institucion.logo_principal', '/assets/logo-dif.png',
     'Ruta del logo principal en los documentos', 'TEXTO', 'institucion', FALSE),
    ('institucion.logo_secundario', '/assets/logo-iba.png',
     'Ruta del logo secundario', 'TEXTO', 'institucion', FALSE),
    ('institucion.color_primario', '#801A36',
     'Color institucional guinda, usado en documentos y portal', 'TEXTO', 'institucion', FALSE),

    -- ---- Patrones de folio ----
    -- Marcadores admitidos: {yyyy} {MM} {dd} {yyyyMMdd} {consecutivo:n}
    -- {cve_inegi} {dependencia} {instituto}
    ('folio.solicitud.patron', 'SOL-{yyyyMMdd}-{consecutivo:5}',
     'Patron del folio de solicitud', 'TEXTO', 'folio', TRUE),
    ('folio.certificado.patron', '{dependencia}/{instituto}/EMP-{cve_inegi}/{consecutivo:5}/{yyyy}',
     'Patron del folio oficial del certificado', 'TEXTO', 'folio', TRUE),
    ('folio.cita.patron', 'CITA-{yyyyMMdd}-{consecutivo:4}',
     'Patron del numero de cita', 'TEXTO', 'folio', TRUE),
    ('folio.animal.patron', '{iniciales_tutor}{nombre_animal:3}{especie:1}{sexo:1}{yyMMdd}',
     'Patron del identificador interno del animal. NO es una CURP.',
     'TEXTO', 'folio', TRUE),
    ('folio.dependencia', 'SEDIF',
     'Siglas de la dependencia para el folio oficial', 'TEXTO', 'folio', TRUE),
    ('folio.instituto', 'IBA',
     'Siglas del instituto para el folio oficial', 'TEXTO', 'folio', TRUE),

    -- ---- Certificado ----
    ('certificado.vigencia_meses', '12',
     'Vigencia del certificado en meses. 0 = sin vencimiento.', 'ENTERO', 'certificado', TRUE),
    ('certificado.leyenda_verificacion',
     'Verifica la autenticidad de este documento escaneando el codigo QR.',
     'Leyenda al pie del certificado', 'TEXTO', 'certificado', FALSE),
    ('certificado.leyenda_sello_fisico',
     'Si requieres el sello institucional impreso, acude a las oficinas del Instituto de Bienestar Animal.',
     'Leyenda mientras la firma electronica avanzada no este disponible',
     'TEXTO', 'certificado', FALSE),
    ('certificado.credenciales_por_hoja', '3',
     'Credenciales por hoja en la impresion por lote (opalina 125 g/m2)',
     'ENTERO', 'certificado', TRUE),

    -- ---- Documentos ----
    ('documento.tamano_maximo_mb', '10',
     'Tamano maximo por archivo en megabytes', 'ENTERO', 'documento', TRUE),
    ('documento.mimes_permitidos', 'application/pdf,image/jpeg,image/png',
     'Tipos MIME aceptados, verificados por contenido', 'TEXTO', 'documento', TRUE),

    -- ---- Portal publico ----
    ('portal.captcha_habilitado', 'true',
     'Exigir captcha en el formulario publico', 'BOOLEANO', 'portal', TRUE),
    ('portal.max_solicitudes_por_ip_dia', '5',
     'Limite de solicitudes por IP al dia; evita que el padron se llene de basura',
     'ENTERO', 'portal', TRUE),
    ('portal.dias_vigencia_token_correccion', '15',
     'Dias que dura la liga de correccion de una solicitud observada',
     'ENTERO', 'portal', TRUE),
    ('portal.telefono_contacto', '',
     'Telefono de atencion mostrado en el portal', 'TEXTO', 'portal', FALSE),
    ('portal.correo_contacto', '',
     'Correo de atencion mostrado en el portal', 'TEXTO', 'portal', FALSE),

    -- ---- Notificaciones ----
    ('notificacion.max_intentos', '3',
     'Reintentos de envio automatico antes de marcar como fallida',
     'ENTERO', 'notificacion', TRUE),

    -- ---- Firma electronica (pendiente de resolucion juridica) ----
    ('firma.habilitada', 'false',
     'Firma electronica avanzada. Mientras sea false, el certificado sale con QR y leyenda.',
     'BOOLEANO', 'firma', TRUE);

-- ---------------------------------------------------------------------------
-- Aviso de privacidad inicial.
-- Es un texto minimo de arranque: el area juridica debe sustituirlo por el
-- aviso institucional formal antes de salir a produccion.
-- ---------------------------------------------------------------------------
INSERT INTO aviso_privacidad (s_version, s_titulo, s_contenido, d_fecha_vigencia, b_vigente) VALUES
    ('1.0',
     'Aviso de Privacidad Simplificado del Padron Estatal de Perros y Gatos',
     'El Sistema Estatal DIF Puebla, a traves del Instituto de Bienestar Animal, '
     'es responsable del tratamiento de los datos personales que proporcione. '
     'Sus datos seran utilizados para integrar el Padron Estatal de Perros y Gatos, '
     'emitir el certificado de empadronamiento y la credencial correspondiente, y '
     'comunicarle el resultado de su tramite. '
     'PENDIENTE: este texto debe sustituirse por el aviso de privacidad integral '
     'que autorice el area juridica antes de la puesta en produccion.',
     CURRENT_DATE, TRUE);

-- ---------------------------------------------------------------------------
-- Plantillas de notificacion. Los textos son editables desde el panel.
-- ---------------------------------------------------------------------------
INSERT INTO plantilla_notificacion
    (s_clave, s_nombre, s_asunto, s_cuerpo, fn_medio_contacto_id, s_variables_disponibles)
SELECT v.clave, v.nombre, v.asunto, v.cuerpo, m.pn_id, v.variables
FROM cat_medio_contacto m,
     (VALUES
        ('SOLICITUD_RECIBIDA',
         'Acuse de recepcion',
         'Recibimos tu solicitud de empadronamiento - Folio {{folio}}',
         E'Hola {{nombre}}:\n\n'
         'Recibimos tu solicitud de empadronamiento para {{nombre_animal}}.\n\n'
         'Folio de seguimiento: {{folio}}\n\n'
         'Puedes consultar el avance de tu tramite en {{url_consulta}} '
         'con tu folio y tu CURP.\n\n'
         'Instituto de Bienestar Animal',
         '{{folio}}, {{nombre}}, {{nombre_animal}}, {{url_consulta}}'),

        ('SOLICITUD_APROBADA',
         'Solicitud aprobada',
         'Tu empadronamiento fue aprobado - Folio {{folio}}',
         E'Hola {{nombre}}:\n\n'
         'Tu solicitud de empadronamiento para {{nombre_animal}} fue APROBADA.\n\n'
         'Folio del certificado: {{folio_certificado}}\n\n'
         'Puedes recoger tu documentacion en:\n'
         '{{sede_nombre}}\n{{sede_domicilio}}\n'
         'Horario: {{sede_horario}}\n'
         'Fecha de tu cita: {{fecha_cita}}\n\n'
         'Instituto de Bienestar Animal',
         '{{folio}}, {{nombre}}, {{nombre_animal}}, {{folio_certificado}}, '
         '{{sede_nombre}}, {{sede_domicilio}}, {{sede_horario}}, {{fecha_cita}}'),

        ('SOLICITUD_OBSERVADA',
         'Solicitud con observaciones',
         'Tu solicitud requiere una correccion - Folio {{folio}}',
         E'Hola {{nombre}}:\n\n'
         'Tu solicitud con folio {{folio}} requiere una correccion:\n\n'
         '{{motivo}}\n{{observacion}}\n\n'
         'Puedes corregirla en la siguiente liga, vigente {{dias_vigencia}} dias:\n'
         '{{url_correccion}}\n\n'
         'Instituto de Bienestar Animal',
         '{{folio}}, {{nombre}}, {{motivo}}, {{observacion}}, {{url_correccion}}, {{dias_vigencia}}'),

        ('SOLICITUD_RECHAZADA',
         'Solicitud rechazada',
         'Resultado de tu solicitud - Folio {{folio}}',
         E'Hola {{nombre}}:\n\n'
         'Tu solicitud con folio {{folio}} no procedio.\n\n'
         'Motivo: {{motivo}}\n{{observacion}}\n\n'
         'Si tienes dudas, comunicate con el Instituto de Bienestar Animal.\n\n'
         'Instituto de Bienestar Animal',
         '{{folio}}, {{nombre}}, {{motivo}}, {{observacion}}')
     ) AS v(clave, nombre, asunto, cuerpo, variables)
WHERE m.s_clave = 'CORREO';

-- ---------------------------------------------------------------------------
-- Contenido del portal publico
-- ---------------------------------------------------------------------------
INSERT INTO contenido_publico (s_clave, s_titulo, s_contenido, s_tipo, n_orden, b_publicado) VALUES
    ('REQUISITOS_GENERALES', 'Requisitos para empadronar',
     E'1. Identificacion oficial vigente (INE) del tutor responsable, legible y por ambos lados.\n'
     '2. Comprobante de domicilio con no mas de tres meses de antiguedad.\n'
     '3. Fotografia clara del animal, de cuerpo completo.\n'
     '4. CURP del tutor (deseable; si no la tienes a la mano puedes continuar).\n'
     '5. Cartilla de vacunacion, si cuentas con ella.',
     'REQUISITOS', 1, TRUE),
    ('FAQ_COSTO', 'El tramite tiene costo?',
     'PENDIENTE: confirmar con el area administrativa.',
     'FAQ', 10, FALSE),
    ('FAQ_SIN_CURP', 'Puedo registrar sin CURP?',
     'Si. Puedes iniciar el tramite con tu nombre completo. El personal completara '
     'tu CURP a partir de la identificacion que presentes.',
     'FAQ', 11, TRUE),
    ('FAQ_TIEMPO', 'Cuanto tarda el tramite?',
     'PENDIENTE: definir el compromiso de tiempo de respuesta con el IBA.',
     'FAQ', 12, FALSE);
