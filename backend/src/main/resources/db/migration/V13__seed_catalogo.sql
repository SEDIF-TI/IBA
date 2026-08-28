-- ============================================================================
-- V13: Semilla de los catalogos operativos.
--
-- Sustituye el texto libre del legado: convivian PERRO y CANINO como especies
-- distintas, 63 razas y 61 colores escritos a mano. Aqui hay un valor por
-- concepto, y el personal puede agregar mas desde el panel.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Especie: UN SOLO nombre por especie.
-- ---------------------------------------------------------------------------
INSERT INTO cat_especie (s_clave, s_nombre, s_descripcion, n_orden) VALUES
    ('CANINO', 'Canino', 'Perro (Canis lupus familiaris)', 1),
    ('FELINO', 'Felino', 'Gato (Felis catus)', 2);

-- ---------------------------------------------------------------------------
-- Sexo
-- ---------------------------------------------------------------------------
INSERT INTO cat_sexo (s_clave, s_nombre, n_orden) VALUES
    ('MACHO',  'Macho',  1),
    ('HEMBRA', 'Hembra', 2);

-- ---------------------------------------------------------------------------
-- Tamano
-- ---------------------------------------------------------------------------
INSERT INTO cat_tamano (s_clave, s_nombre, s_descripcion, n_orden) VALUES
    ('CHICO',   'Chico',   'Hasta 10 kg aproximadamente',  1),
    ('MEDIANO', 'Mediano', 'Entre 10 y 25 kg aproximadamente', 2),
    ('GRANDE',  'Grande',  'Mas de 25 kg aproximadamente', 3);

-- ---------------------------------------------------------------------------
-- Razas caninas. Mestizo/Criollo va primero: es la mas frecuente del padron.
-- ---------------------------------------------------------------------------
INSERT INTO cat_raza (fn_especie_id, s_clave, s_nombre, n_orden)
SELECT e.pn_id, v.clave, v.nombre, v.orden
FROM cat_especie e,
     (VALUES
        ('CAN_MESTIZO',        'Mestizo / Criollo',       1),
        ('CAN_CHIHUAHUA',      'Chihuahua',               2),
        ('CAN_PASTOR_ALEMAN',  'Pastor Aleman',           3),
        ('CAN_LABRADOR',       'Labrador Retriever',      4),
        ('CAN_POODLE',         'Poodle / French Poodle',  5),
        ('CAN_PITBULL',        'Pit Bull Terrier',        6),
        ('CAN_SCHNAUZER',      'Schnauzer',               7),
        ('CAN_CHIHUAHUENO',    'Xoloitzcuintle',          8),
        ('CAN_BOXER',          'Boxer',                   9),
        ('CAN_BEAGLE',         'Beagle',                 10),
        ('CAN_COCKER',         'Cocker Spaniel',         11),
        ('CAN_DALMATA',        'Dalmata',                12),
        ('CAN_DOBERMAN',       'Doberman',               13),
        ('CAN_GOLDEN',         'Golden Retriever',       14),
        ('CAN_HUSKY',          'Husky Siberiano',        15),
        ('CAN_MALTES',         'Maltes',                 16),
        ('CAN_PUG',            'Pug',                    17),
        ('CAN_ROTTWEILER',     'Rottweiler',             18),
        ('CAN_SALCHICHA',      'Dachshund / Salchicha',  19),
        ('CAN_SHIH_TZU',       'Shih Tzu',               20),
        ('CAN_YORKSHIRE',      'Yorkshire Terrier',      21),
        ('CAN_BULLDOG',        'Bulldog',                22),
        ('CAN_BORDER_COLLIE',  'Border Collie',          23),
        ('CAN_GRAN_DANES',     'Gran Danes',             24),
        ('CAN_SAN_BERNARDO',   'San Bernardo',           25),
        ('CAN_OTRA',           'Otra raza canina',       99)
     ) AS v(clave, nombre, orden)
WHERE e.s_clave = 'CANINO';

-- ---------------------------------------------------------------------------
-- Razas felinas
-- ---------------------------------------------------------------------------
INSERT INTO cat_raza (fn_especie_id, s_clave, s_nombre, n_orden)
SELECT e.pn_id, v.clave, v.nombre, v.orden
FROM cat_especie e,
     (VALUES
        ('FEL_MESTIZO',    'Mestizo / Criollo',       1),
        ('FEL_ANGORA',     'Angora',                  2),
        ('FEL_PERSA',      'Persa',                   3),
        ('FEL_SIAMES',     'Siames',                  4),
        ('FEL_MAINE_COON', 'Maine Coon',              5),
        ('FEL_BENGALI',    'Bengali',                 6),
        ('FEL_ESFINGE',    'Esfinge / Sphynx',        7),
        ('FEL_BRITANICO',  'British Shorthair',       8),
        ('FEL_RAGDOLL',    'Ragdoll',                 9),
        ('FEL_OTRA',       'Otra raza felina',       99)
     ) AS v(clave, nombre, orden)
WHERE e.s_clave = 'FELINO';

-- ---------------------------------------------------------------------------
-- Colores
-- ---------------------------------------------------------------------------
INSERT INTO cat_color (s_clave, s_nombre, n_orden) VALUES
    ('NEGRO',        'Negro',              1),
    ('BLANCO',       'Blanco',             2),
    ('CAFE',         'Cafe',               3),
    ('GRIS',         'Gris',               4),
    ('DORADO',       'Dorado',             5),
    ('AMARILLO',     'Amarillo',           6),
    ('BEIGE',        'Beige',              7),
    ('NARANJA',      'Naranja',            8),
    ('ATIGRADO',     'Atigrado',           9),
    ('MANCHADO',     'Manchado',          10),
    ('BLANCO_NEGRO', 'Blanco con negro',  11),
    ('BLANCO_CAFE',  'Blanco con cafe',   12),
    ('TRICOLOR',     'Tricolor',          13),
    ('CAREY',        'Carey',             14),
    ('OTRO',         'Otro color',        99);

-- ---------------------------------------------------------------------------
-- Metodo de identificacion
-- ---------------------------------------------------------------------------
INSERT INTO cat_metodo_identificacion (s_clave, s_nombre, b_requiere_identificador, n_orden) VALUES
    ('NINGUNO',   'Ninguno',   FALSE, 1),
    ('PLACA',     'Placa',     TRUE,  2),
    ('MICROCHIP', 'Microchip', TRUE,  3),
    ('TATUAJE',   'Tatuaje',   TRUE,  4);

-- ---------------------------------------------------------------------------
-- Medio de contacto.
-- b_automatizable = FALSE significa que el aviso lo tiene que dar una persona:
-- esas notificaciones caen en la bandeja de contacto manual.
-- ---------------------------------------------------------------------------
INSERT INTO cat_medio_contacto (s_clave, s_nombre, b_automatizable, n_orden) VALUES
    ('CORREO',   'Correo electronico', TRUE,  1),
    ('TELEFONO', 'Telefono',           FALSE, 2),
    ('WHATSAPP', 'WhatsApp',           FALSE, 3);

-- ---------------------------------------------------------------------------
-- Tipos de documento
-- ---------------------------------------------------------------------------
INSERT INTO cat_tipo_documento
    (s_clave, s_nombre, s_descripcion, b_requerido_persona, b_requerido_animal,
     n_tamano_maximo_mb, s_extensiones_permitidas, n_orden) VALUES
    ('INE', 'Identificacion oficial (INE)',
     'Credencial para votar vigente, por ambos lados y legible',
     TRUE,  FALSE, 5, 'pdf,jpg,jpeg,png', 1),
    ('CURP', 'Constancia de CURP',
     'Impresion de la CURP emitida por RENAPO',
     FALSE, FALSE, 5, 'pdf,jpg,jpeg,png', 2),
    ('COMPROBANTE_DOMICILIO', 'Comprobante de domicilio',
     'No mayor a tres meses de antiguedad',
     TRUE,  FALSE, 5, 'pdf,jpg,jpeg,png', 3),
    ('FOTOGRAFIA_ANIMAL', 'Fotografia del animal',
     'Foto de cuerpo completo, clara y reciente. Se imprime en la credencial.',
     FALSE, TRUE,  8, 'jpg,jpeg,png', 4),
    ('CARTILLA_VACUNACION', 'Cartilla de vacunacion',
     'Cartilla con las vacunas aplicadas',
     FALSE, FALSE, 5, 'pdf,jpg,jpeg,png', 5),
    ('COMPROBANTE_ANTIRRABICO', 'Comprobante de vacuna antirrabica',
     'Constancia de aplicacion de la vacuna antirrabica',
     FALSE, FALSE, 5, 'pdf,jpg,jpeg,png', 6),
    ('CERTIFICADO_ADOPCION', 'Certificado de adopcion',
     'Documento que acredita la adopcion',
     FALSE, FALSE, 5, 'pdf,jpg,jpeg,png', 7);

-- ---------------------------------------------------------------------------
-- Estatus de la solicitud (maquina de estados).
-- Los colores corresponden a la paleta institucional.
-- ---------------------------------------------------------------------------
INSERT INTO cat_estatus_solicitud
    (s_clave, s_nombre, s_descripcion, b_terminal, b_requiere_motivo, s_color_ui, n_orden) VALUES
    ('REGISTRADA', 'Registrada',
     'Recibida por el portal, la jornada o la ventanilla. Aun no se revisa.',
     FALSE, FALSE, '#0369A1', 1),
    ('EN_REVISION', 'En revision',
     'El personal esta cotejando datos y documentos.',
     FALSE, FALSE, '#B45309', 2),
    ('OBSERVADA', 'Observada',
     'Requiere correccion de la persona. Se le envia liga con token.',
     FALSE, TRUE,  '#B45309', 3),
    ('APROBADA', 'Aprobada',
     'Procede el empadronamiento. Se genera certificado y se asigna cita.',
     FALSE, FALSE, '#15803D', 4),
    ('CERTIFICADO_EMITIDO', 'Certificado emitido',
     'El documento ya se genero y esta listo para entrega.',
     FALSE, FALSE, '#15803D', 5),
    ('ENTREGADA', 'Entregada',
     'La persona recogio su documentacion. Tramite concluido.',
     TRUE,  FALSE, '#15803D', 6),
    ('RECHAZADA', 'Rechazada',
     'No procede. Requiere motivo del catalogo.',
     TRUE,  TRUE,  '#C62828', 7),
    ('CANCELADA', 'Cancelada',
     'Cancelada antes de concluir.',
     TRUE,  TRUE,  '#64748B', 8);

-- ---------------------------------------------------------------------------
-- Estatus de la cita
-- ---------------------------------------------------------------------------
INSERT INTO cat_estatus_cita (s_clave, s_nombre, b_terminal, s_color_ui, n_orden) VALUES
    ('AGENDADA',   'Agendada',   FALSE, '#0369A1', 1),
    ('ATENDIDA',   'Atendida',   TRUE,  '#15803D', 2),
    ('NO_ASISTIO', 'No asistio', TRUE,  '#C62828', 3),
    ('REAGENDADA', 'Reagendada', TRUE,  '#B45309', 4);

-- ---------------------------------------------------------------------------
-- Motivos de rechazo u observacion.
-- b_permite_correccion = TRUE: la persona puede corregir (pasa a OBSERVADA).
-- FALSE: causa de rechazo definitivo.
-- ---------------------------------------------------------------------------
INSERT INTO cat_motivo_rechazo (s_clave, s_nombre, s_descripcion, b_permite_correccion, n_orden) VALUES
    ('INE_ILEGIBLE', 'Identificacion ilegible',
     'La INE no se lee con claridad. Es el motivo mas frecuente.', TRUE, 1),
    ('INE_VENCIDA', 'Identificacion vencida',
     'La credencial no esta vigente.', TRUE, 2),
    ('COMPROBANTE_ILEGIBLE', 'Comprobante de domicilio ilegible',
     'El comprobante no se lee con claridad.', TRUE, 3),
    ('COMPROBANTE_ANTIGUO', 'Comprobante de domicilio con mas de tres meses',
     'Se requiere uno reciente.', TRUE, 4),
    ('DOCUMENTO_FALTANTE', 'Falta documentacion',
     'No se adjuntaron todos los documentos requeridos.', TRUE, 5),
    ('FOTO_ANIMAL_ILEGIBLE', 'Fotografia del animal no valida',
     'La foto no permite identificar al animal.', TRUE, 6),
    ('DATOS_INCOMPLETOS', 'Datos incompletos',
     'Falta informacion en el formulario.', TRUE, 7),
    ('DATOS_NO_COINCIDEN', 'Los datos no coinciden con los documentos',
     'Lo capturado no corresponde con lo que muestran los documentos.', TRUE, 8),
    ('CURP_INVALIDA', 'CURP invalida o no corresponde',
     'La CURP no es valida o no corresponde a la persona.', TRUE, 9),
    ('DUPLICADO', 'Registro duplicado',
     'El animal ya esta empadronado.', FALSE, 10),
    ('FUERA_JURISDICCION', 'Domicilio fuera del estado de Puebla',
     'El padron es estatal.', FALSE, 11),
    ('ESPECIE_NO_APLICA', 'La especie no aplica para este padron',
     'Solo se empadronan perros y gatos.', FALSE, 12),
    ('SOLICITUD_APOCRIFA', 'Solicitud apocrifa o con datos falsos',
     'Se detectaron datos falsos o suplantacion.', FALSE, 13),
    ('DESISTIMIENTO', 'Desistimiento de la persona solicitante',
     'La persona pidio cancelar el tramite.', FALSE, 14);
