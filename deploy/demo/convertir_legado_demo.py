#!/usr/bin/env python3
"""
Convierte el volcado MySQL del sistema anterior de empadronamiento en un script SQL de carga para
la base del SEA, para montar una demostración con los datos del padrón.

Área: Núcleo compartido (herramienta de despliegue de demos). No forma parte de la aplicación.

Por omisión carga los DATOS REALES, para cotejarlos con la documentación (INE, comprobantes):
    - Los 216 ciudadanos registrados, tengan o no mascota, con nombre (separado en nombre y
      apellidos), correo, teléfono, calle, número, colonia, código postal y municipio.
    - Las mascotas con todos sus datos y su clave original del sistema anterior.
    - Los trámites con sus folios, los certificados (incluido el cancelado), las solicitudes de
      corrección en el expediente y los avisos enviados por correo.
    La CURP no se guardaba como dato en el sistema anterior (solo el documento escaneado): queda
    vacía y la persona aparece con identidad pendiente de cotejo.

Con --anonimizar reemplaza nombre, correo, teléfono, calle, número y CURP por datos ficticios
(para presentaciones fuera de la institución).

No importa los administradores ni las contraseñas del sistema anterior, ni los archivos de
documentos (el volcado solo trae sus rutas en el servidor anterior).

Uso
    python3 deploy/demo/convertir_legado_demo.py <volcado-mysql.sql> <salida.sql> [--anonimizar]
    Luego se carga con:
    docker exec -i sea-db psql -U <POSTGRES_USER> -d <POSTGRES_DB> -v ON_ERROR_STOP=1 < salida.sql

Efectos
    Solo escribe el archivo de salida. El SQL generado se niega a cargarse si la base ya tiene
    tutores, corre en una sola transacción y deja una entrada en la bitácora. Con datos reales, el
    archivo de salida contiene datos personales: se guarda fuera del proyecto y nunca va a git.
"""
import random
import re
import sys
import unicodedata
from collections import defaultdict
from datetime import date, datetime, timedelta

SEMILLA = 20261118
AZAR = random.Random(SEMILLA)

# ---------------------------------------------------------------------------------------------
# Lectura del volcado de MySQL
# ---------------------------------------------------------------------------------------------
ESCAPES = {'n': '\n', 'r': '\r', 't': '\t', '0': '\0', "'": "'", '"': '"', '\\': '\\', 'Z': '\x1a'}


def _filas(texto):
    """Parte VALUES (...),(...) respetando comillas y escapes de MySQL."""
    filas, fila, campo = [], [], []
    en_texto = en_fila = es_cadena = False
    i = 0
    while i < len(texto):
        c = texto[i]
        if en_texto:
            if c == '\\':
                campo.append(ESCAPES.get(texto[i + 1], texto[i + 1]))
                i += 2
                continue
            if c == "'":
                if texto[i + 1:i + 2] == "'":
                    campo.append("'")
                    i += 2
                    continue
                en_texto = False
            else:
                campo.append(c)
        elif c == "'":
            en_texto = es_cadena = True
        elif c == '(' and not en_fila:
            en_fila, fila, campo, es_cadena = True, [], [], False
        elif c == ',' and en_fila:
            fila.append(_valor(campo, es_cadena))
            campo, es_cadena = [], False
        elif c == ')' and en_fila:
            fila.append(_valor(campo, es_cadena))
            filas.append(fila)
            en_fila = False
        elif en_fila:
            campo.append(c)
        i += 1
    return filas


def _valor(campo, es_cadena):
    valor = ''.join(campo)
    if es_cadena:
        return valor
    valor = valor.strip()
    return None if valor.upper() == 'NULL' else valor


def leer_volcado(ruta):
    """Devuelve {tabla: [ {columna: valor} ]} a partir de un volcado de mysqldump."""
    texto = open(ruta, encoding='utf-8').read()
    tablas = {}
    for m in re.finditer(r"CREATE TABLE `(\w+)` \((.*?)\n\) ENGINE", texto, re.S):
        nombre = m.group(1)
        columnas = re.findall(r"^\s+`(\w+)`", m.group(2), re.M)
        filas = []
        for ins in re.finditer(r"INSERT INTO `%s` VALUES (.*?);\n" % nombre, texto, re.S):
            filas.extend(dict(zip(columnas, f)) for f in _filas(ins.group(1)))
        tablas[nombre] = filas
    return tablas


# ---------------------------------------------------------------------------------------------
# Utilidades de texto y SQL
# ---------------------------------------------------------------------------------------------
def sin_acentos(texto):
    return ''.join(c for c in unicodedata.normalize('NFD', texto or '')
                   if unicodedata.category(c) != 'Mn')


def normal(texto):
    """Mayúsculas, sin acentos y con espacios simples, para comparar textos libres."""
    return re.sub(r'\s+', ' ', sin_acentos(texto).upper()).strip()


def sql(valor):
    """Literal SQL seguro para PostgreSQL."""
    if valor is None:
        return 'NULL'
    if isinstance(valor, bool):
        return 'TRUE' if valor else 'FALSE'
    if isinstance(valor, (int, float)):
        return str(valor)
    return "'" + str(valor).replace("'", "''") + "'"


def fecha_utc(texto):
    """Las fechas del volcado vienen en UTC (mysqldump con TIME_ZONE='+00:00')."""
    if not texto or texto.startswith('0000'):
        return None
    return datetime.strptime(texto[:19], '%Y-%m-%d %H:%M:%S')


def ts(dt):
    return sql(dt.strftime('%Y-%m-%d %H:%M:%S') + '+00') if dt else 'NULL'


# ---------------------------------------------------------------------------------------------
# Personas ficticias
# ---------------------------------------------------------------------------------------------
NOMBRES_M = ['JUAN', 'LUIS', 'CARLOS', 'JORGE', 'MIGUEL', 'RICARDO', 'FERNANDO', 'EDUARDO', 'DANIEL',
             'ALEJANDRO', 'ROBERTO', 'SERGIO', 'ARTURO', 'RAUL', 'HECTOR', 'OSCAR', 'PABLO', 'ADRIAN',
             'GERARDO', 'MARIO', 'RAFAEL', 'ANDRES', 'DIEGO', 'EMILIO', 'IVAN', 'JAVIER', 'RODRIGO',
             'ALBERTO', 'MANUEL', 'VICTOR']
NOMBRES_F = ['MARIA', 'GUADALUPE', 'ANA', 'LAURA', 'PATRICIA', 'GABRIELA', 'VERONICA', 'ADRIANA',
             'SANDRA', 'ELIZABETH', 'ROSA', 'CLAUDIA', 'DANIELA', 'FERNANDA', 'MONICA', 'SOFIA',
             'ANDREA', 'KARLA', 'LETICIA', 'DIANA', 'PAOLA', 'ALEJANDRA', 'BEATRIZ', 'CAROLINA',
             'SILVIA', 'TERESA', 'LORENA', 'NANCY', 'ROCIO', 'XIMENA']
APELLIDOS = ['HERNANDEZ', 'GARCIA', 'MARTINEZ', 'LOPEZ', 'GONZALEZ', 'PEREZ', 'RODRIGUEZ', 'SANCHEZ',
             'RAMIREZ', 'CRUZ', 'FLORES', 'GOMEZ', 'MORALES', 'VAZQUEZ', 'REYES', 'JIMENEZ', 'TORRES',
             'DIAZ', 'GUTIERREZ', 'RUIZ', 'MENDOZA', 'AGUILAR', 'ORTIZ', 'MORENO', 'CASTILLO',
             'ROMERO', 'ALVAREZ', 'MENDEZ', 'CHAVEZ', 'RIVERA', 'JUAREZ', 'RAMOS', 'DOMINGUEZ',
             'HERRERA', 'MEDINA', 'CASTRO', 'VARGAS', 'GUZMAN', 'VELAZQUEZ', 'ROJAS', 'SALAZAR',
             'CONTRERAS', 'LUNA', 'ORTEGA', 'CORTES', 'ROSALES', 'SOLIS', 'NAVARRO', 'CERVANTES',
             'TAPIA', 'CABRERA', 'MOLINA', 'CAMPOS', 'VEGA', 'SOTO', 'PACHECO', 'FUENTES', 'BRAVO']
CALLES = ['AVENIDA REFORMA', 'CALLE 5 DE MAYO', 'CALLE HIDALGO', 'AVENIDA JUAREZ', 'CALLE MORELOS',
          'CALLE ALLENDE', 'CALLE ZARAGOZA', 'AVENIDA INDEPENDENCIA', 'CALLE GUERRERO',
          'CALLE ALDAMA', 'PRIVADA DE LOS PINOS', 'CALLE DE LAS FLORES', 'CERRADA DEL SOL',
          'AVENIDA DE LA PAZ', 'CALLE NIÑOS HEROES', 'CALLE 16 DE SEPTIEMBRE', 'CALLE BENITO JUAREZ',
          'CALLE LOS FRESNOS', 'CALLE CEDROS', 'AVENIDA LAS TORRES']
# Palabras que RENAPO no permite en las cuatro primeras letras de la CURP.
INCONVENIENTES = {'BACA', 'BAKA', 'BUEI', 'BUEY', 'CACA', 'CACO', 'CAGA', 'CAGO', 'CAKA', 'CAKO',
                  'COGE', 'COGI', 'COJA', 'COJE', 'COJI', 'COJO', 'COLA', 'CULO', 'FALO', 'FETO',
                  'GETA', 'GUEI', 'GUEY', 'JETA', 'JOTO', 'KACA', 'KACO', 'KAGA', 'KAGO', 'KAKA',
                  'KAKO', 'KOGE', 'KOGI', 'KOJA', 'KOJE', 'KOJI', 'KOJO', 'KOLA', 'KULO', 'LILO',
                  'LOCA', 'LOCO', 'LOKA', 'LOKO', 'MAME', 'MAMO', 'MEAR', 'MEAS', 'MEON', 'MIAR',
                  'MION', 'MOCO', 'MOKO', 'MULA', 'MULO', 'NACA', 'NACO', 'PEDA', 'PEDO', 'PENE',
                  'PIPI', 'PITO', 'POPO', 'PUTA', 'PUTO', 'QULO', 'RATA', 'ROBA', 'ROBE', 'ROBO',
                  'RUIN', 'SENO', 'TETA', 'VACA', 'VAGA', 'VAGO', 'VAKA', 'VUEI', 'VUEY', 'WUEI',
                  'WUEY'}
ALFABETO_CURP = '0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ'


def _vocal_interna(texto):
    return next((c for c in texto[1:] if c in 'AEIOU'), 'X')


def _consonante_interna(texto):
    return next((c for c in texto[1:] if c.isalpha() and c not in 'AEIOU'), 'X')


def curp_ficticia(nombre, paterno, materno, nacimiento, sexo, usadas):
    """CURP con estructura, entidad (PL) y dígito verificador válidos, igual que CurpUtil.esValida."""
    primer_nombre = nombre.split()[1] if nombre.split()[0] in ('MARIA', 'JOSE') and len(nombre.split()) > 1 \
        else nombre.split()[0]
    inicio = paterno[0] + _vocal_interna(paterno) + materno[0] + primer_nombre[0]
    if inicio in INCONVENIENTES:
        inicio = inicio[0] + 'X' + inicio[2:]
    cuerpo = (inicio + nacimiento.strftime('%y%m%d') + sexo + 'PL'
              + _consonante_interna(paterno) + _consonante_interna(materno)
              + _consonante_interna(primer_nombre))
    for intento in range(10):
        homoclave = str(intento) if nacimiento.year < 2000 else 'ABCDEFGHIJ'[intento]
        base = cuerpo + homoclave
        suma = sum(ALFABETO_CURP.index(c) * (18 - i) for i, c in enumerate(base))
        curp = base + str((10 - suma % 10) % 10)
        if curp not in usadas:
            usadas.add(curp)
            return curp
    raise ValueError('No se pudo generar una CURP única')


def persona_ficticia(usadas):
    sexo = AZAR.choice('HM')
    nombre = AZAR.choice(NOMBRES_M if sexo == 'H' else NOMBRES_F)
    if AZAR.random() < 0.25:
        nombre += ' ' + AZAR.choice(NOMBRES_M if sexo == 'H' else NOMBRES_F)
    paterno, materno = AZAR.sample(APELLIDOS, 2)
    nacimiento = date(1960, 1, 1) + timedelta(days=AZAR.randint(0, 365 * 44))
    return {
        'nombre': nombre, 'paterno': paterno, 'materno': materno, 'sexo': sexo,
        'nacimiento': nacimiento,
        'curp': curp_ficticia(nombre, paterno, materno, nacimiento, sexo, usadas),
        # Dominio reservado para ejemplos: no llega a nadie.
        'correo': f"{nombre.split()[0].lower()}.{paterno.lower()}{AZAR.randint(10, 99)}@ejemplo.com",
        # Central 000 dentro de la lada 222: no existe en la numeración mexicana.
        'telefono': '222000' + f"{AZAR.randint(0, 9999):04d}",
        'calle': AZAR.choice(CALLES),
        'numero': str(AZAR.randint(1, 450)),
    }


PARTICULAS = {'DE', 'DEL', 'LA', 'LAS', 'LOS', 'Y', 'SAN', 'MC', 'VAN', 'VON'}


def separar_nombre(completo):
    """'DIANA VANESSA DE LOS SANTOS DIAZ' -> ('DIANA VANESSA', 'DE LOS SANTOS', 'DIAZ').

    El sistema anterior guardaba el nombre en un solo campo con el orden NOMBRES PATERNO MATERNO.
    Se toman los apellidos desde el final, uniendo las partículas (DE, DEL, LA, LOS...).
    """
    partes = re.sub(r'\s+', ' ', (completo or '').strip().upper()).split(' ')
    if len(partes) == 1:
        return partes[0] or 'SIN NOMBRE', 'SIN APELLIDO', None
    if len(partes) == 2:
        return partes[0], partes[1], None

    def tomar_apellido(lista):
        apellido = [lista.pop()]
        while lista and lista[-1] in PARTICULAS and len(lista) > 1:
            apellido.insert(0, lista.pop())
        return ' '.join(apellido)

    materno = tomar_apellido(partes)
    paterno = tomar_apellido(partes) if len(partes) > 1 else None
    if paterno is None:
        return partes[0], materno, None
    return ' '.join(partes), paterno, materno


def persona_real(usuario):
    nombre, paterno, materno = separar_nombre(usuario['nombre'])
    telefono = re.sub(r'\D', '', usuario['telefono'] or '')
    return {
        'nombre': nombre[:120], 'paterno': paterno[:120], 'materno': (materno or '')[:120] or None,
        'sexo': None, 'nacimiento': None, 'curp': None,
        'correo': (usuario['correo'] or '').strip().lower() or None,
        'telefono': telefono if len(telefono) == 10 else None,
        'calle': ((usuario['calle'] or '').strip()[:200]) or None,
        'numero': ((usuario['numero'] or '').strip()[:20]) or None,
    }


# ---------------------------------------------------------------------------------------------
# Homologación de valores libres del sistema anterior a los catálogos del SEA
# ---------------------------------------------------------------------------------------------
RAZAS = [  # (palabra clave en el texto normalizado, clave de catálogo)
    ('CHIHUAHUENO', 'CAN_CHIHUAHUENO'), ('CHIHUAHUA', 'CAN_CHIHUAHUA'),
    ('PASTOR ALEMAN', 'CAN_PASTOR_ALEMAN'), ('LABRADOR', 'CAN_LABRADOR'), ('POODLE', 'CAN_POODLE'),
    ('PODLE', 'CAN_POODLE'), ('PITBULL', 'CAN_PITBULL'), ('PIT BULL', 'CAN_PITBULL'),
    ('SCHNAUZER', 'CAN_SCHNAUZER'), ('BOXER', 'CAN_BOXER'), ('BEAGLE', 'CAN_BEAGLE'),
    ('COCKER', 'CAN_COCKER'), ('DALMATA', 'CAN_DALMATA'), ('DOBERMAN', 'CAN_DOBERMAN'),
    ('GOLDEN', 'CAN_GOLDEN'), ('HUSKY', 'CAN_HUSKY'), ('MALTES', 'CAN_MALTES'), ('PUG', 'CAN_PUG'),
    ('ROTWEILLER', 'CAN_ROTTWEILER'), ('ROTTWEILER', 'CAN_ROTTWEILER'), ('SALCHICHA', 'CAN_SALCHICHA'),
    ('SHIH', 'CAN_SHIH_TZU'), ('SHIHTZU', 'CAN_SHIH_TZU'), ('YORK', 'CAN_YORKSHIRE'),
    ('BULLDOG', 'CAN_BULLDOG'), ('BORDER COLLIE', 'CAN_BORDER_COLLIE'), ('GRAN DANES', 'CAN_GRAN_DANES'),
    ('SAN BERNARDO', 'CAN_SAN_BERNARDO'),
    ('SIAMES', 'FEL_SIAMES'), ('ANGORA', 'FEL_ANGORA'), ('PERSA', 'FEL_PERSA'),
    ('MAINE', 'FEL_MAINE_COON'), ('BENGAL', 'FEL_BENGALI'), ('ESFINGE', 'FEL_ESFINGE'),
    ('SPHYNX', 'FEL_ESFINGE'), ('BRITANICO', 'FEL_BRITANICO'), ('RAGDOLL', 'FEL_RAGDOLL'),
]
MESTIZO = ('MESTIZ', 'MEZTIZ', 'MIXT', 'CRIOLL', 'CRUZA', 'DOMESTICO', 'COMUN', 'TABY', 'TABI',
           'ATIGRADO', 'EUROPEO', 'SIN RAZA')

COLORES = [  # (condición sobre el texto normalizado, clave de catálogo); se aplica la primera
    (lambda c: 'CAREY' in c, 'CAREY'),
    (lambda c: 'CALICO' in c or 'TRICOLOR' in c, 'TRICOLOR'),
    (lambda c: 'ATIGRAD' in c, 'ATIGRADO'),
    (lambda c: 'BLANCO' in c and 'NEGRO' in c and 'CAFE' not in c, 'BLANCO_NEGRO'),
    (lambda c: ('BLANCO' in c or 'BLANCA' in c) and 'CAFE' in c, 'BLANCO_CAFE'),
    (lambda c: 'MANCHA' in c, 'MANCHADO'),
    (lambda c: any(p in c for p in ('MIEL', 'DORAD', 'ALBARICOQUE')), 'DORADO'),
    (lambda c: 'NARANJ' in c or 'ANARANJ' in c, 'NARANJA'),
    (lambda c: 'BEIGE' in c or 'CREMA' in c, 'BEIGE'),
    (lambda c: 'AMARILL' in c, 'AMARILLO'),
    (lambda c: 'GRIS' in c or 'PLATA' in c, 'GRIS'),
    (lambda c: c.startswith('CAFE'), 'CAFE'),
    (lambda c: c.startswith('NEGR'), 'NEGRO'),
    (lambda c: c.startswith('BLANC'), 'BLANCO'),
]


def homologar_raza(texto, especie):
    t = normal(texto)
    for clave_texto, clave in RAZAS:
        if clave_texto in t and clave.startswith('CAN' if especie == 'CANINO' else 'FEL'):
            return clave, None
    prefijo = 'CAN' if especie == 'CANINO' else 'FEL'
    if not t or any(p in t for p in MESTIZO):
        return f'{prefijo}_MESTIZO', (texto.strip() if t and t not in ('MESTIZO', 'MESTIZA') else None)
    return f'{prefijo}_OTRA', texto.strip()


def homologar_color(texto):
    c = normal(texto)
    if not c:
        return None, None
    for condicion, clave in COLORES:
        if condicion(c):
            # Si el texto decía más que la clave del catálogo, se conserva en las características.
            exacto = c.replace(' ', '_') == clave or c == clave.replace('_', ' CON ')
            return clave, None if exacto else texto.strip()
    return 'OTRO', texto.strip()


def edad_en_meses(texto):
    """'6' = años; '2 MESES'; '1 AÑO 1 MES'; '12 AÑOS 11'; '8 AÑOS 2 M'."""
    t = normal(texto)
    if not t:
        return None
    if t.isdigit():
        return min(int(t) * 12, 600)
    anios = re.search(r'(\d+)\s*AN', t)
    meses = re.search(r'(\d+)\s*M', t)
    if anios:
        total = int(anios.group(1)) * 12
        resto = re.findall(r'\d+', t[anios.end():])
        if meses and meses.start() > anios.start():
            total += int(meses.group(1))
        elif resto:
            total += int(resto[0])
        return min(total, 600)
    if meses:
        return min(int(meses.group(1)), 600)
    numeros = re.findall(r'\d+', t)
    return min(int(numeros[0]) * 12, 600) if numeros else None


def clave_animal(persona, nombre_animal, especie, sexo, fecha, usadas):
    """Mismo patrón que FolioService.generarClaveAnimal; si se repite, se agrega un dígito."""
    tres = re.sub(r'[^A-Z0-9]', '', normal(nombre_animal))
    tres = (tres + 'XXX')[:3]
    base = (persona['paterno'][0] + persona['nombre'][0] + tres + ('F' if especie == 'FELINO' else 'C')
            + ('H' if sexo == 'HEMBRA' else 'M') + fecha.strftime('%y%m%d'))
    clave, n = base, 1
    while clave in usadas:
        clave, n = f'{base}{n}', n + 1
    usadas.add(clave)
    return clave


# ---------------------------------------------------------------------------------------------
# Generación del script
# ---------------------------------------------------------------------------------------------
def generar(tablas, anonimizar=False):
    mascotas = [m for m in tablas['mascotas']]
    tutores_con_mascota = {m['id_usuario'] for m in mascotas}
    # Con datos reales van todas las personas registradas; anonimizado, solo quienes tienen mascota.
    usuarios = {u['id_usuario']: u for u in tablas['usuarios']
                if not anonimizar or u['id_usuario'] in tutores_con_mascota}
    tramites = {t['id_mascota']: t for t in tablas['temporal']}
    certificados = {c['id_mascota']: c for c in tablas['certificados']}
    correcciones = {n['id_usuario'] for n in tablas['notificaciones']
                    if (n['asunto'] or '').upper().startswith('SOLICITAR CORRECCION')}

    curps, claves = set(), set()
    personas = {uid: (persona_ficticia(curps) if anonimizar else persona_real(usuarios[uid]))
                for uid in sorted(usuarios, key=int)}

    salida = []
    escribir = salida.append
    resumen = defaultdict(int)

    modo = ('Tutores ANONIMIZADOS (nombres, correos, teléfonos, calles, números y CURP ficticios).'
            if anonimizar else 'DATOS REALES del sistema anterior: contiene datos personales.')
    escribir("""-- =============================================================================
-- SEA - Carga de DEMOSTRACIÓN a partir del sistema anterior de empadronamiento.
-- Generado por deploy/demo/convertir_legado_demo.py.
-- {MODO}
-- Se carga como superusuario de la base; no se sube a git.
-- =============================================================================
\\set ON_ERROR_STOP on
BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM persona) THEN
        RAISE EXCEPTION 'La base ya tiene tutores: la carga de demostracion solo va en una base sin tutores.';
    END IF;
END $$;

CREATE TEMP TABLE mapa_persona (legado INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;
CREATE TEMP TABLE mapa_animal (legado INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;
CREATE TEMP TABLE mapa_solicitud (legado INTEGER PRIMARY KEY, id BIGINT NOT NULL) ON COMMIT DROP;

-- Personal que figura como responsable de las resoluciones importadas.
CREATE TEMP TABLE personal_demo ON COMMIT DROP AS
SELECT COALESCE((SELECT pn_id FROM usuario WHERE s_usuario = 'demo.iba'),
                (SELECT min(pn_id) FROM usuario)) AS id;
""")

    # ---- Tutores ----
    fechas_tutor = defaultdict(list)
    for m in mascotas:
        f = fecha_utc(m['fecha_alta'])
        if f:
            fechas_tutor[m['id_usuario']].append(f)
    for uid, u in sorted(usuarios.items(), key=lambda x: int(x[0])):
        p = personas[uid]
        alta = min(fechas_tutor[uid]) if fechas_tutor[uid] else fecha_utc(u['fecha_alta'])
        # Identidad validada solo si hay CURP y la persona obtuvo certificado.
        con_certificado = p['curp'] is not None and any(
            certificados.get(m['id_mascota']) for m in mascotas if m['id_usuario'] == uid)
        cp = str(u['cp']) if u['cp'] and len(str(u['cp'])) == 5 else None
        escribir(f"""WITH n AS (INSERT INTO persona (s_curp, b_identidad_validada, s_nombre, s_apellido_paterno,
    s_apellido_materno, s_correo, s_telefono, fn_medio_contacto_preferido_id, fn_municipio_id, s_calle,
    s_numero_exterior, s_colonia, s_codigo_postal, d_fecha_nacimiento, s_sexo_curp, s_entidad_nacimiento,
    b_activo, d_fecha_creacion)
  VALUES ({sql(p['curp'])}, {sql(con_certificado)}, {sql(p['nombre'])}, {sql(p['paterno'])}, {sql(p['materno'])},
    {sql(p['correo'])}, {sql(p['telefono'])},
    (SELECT pn_id FROM cat_medio_contacto WHERE s_clave = {sql('CORREO' if p['correo'] else 'TELEFONO')}),
    (SELECT pn_id FROM cat_municipio WHERE s_cve_inegi = {sql(str(u['cve_entidad']))}), {sql(p['calle'])},
    {sql(p['numero'])}, {sql((u['colonia'] or '').strip()[:150] or None)}, {sql(cp)},
    {sql(p['nacimiento'].isoformat() if p['nacimiento'] else None)}, {sql(p['sexo'])},
    {sql('PL' if p['curp'] else None)}, {sql(u['activo'] != '0')}, {ts(alta)})
  RETURNING pn_id)
INSERT INTO mapa_persona SELECT {int(uid)}, pn_id FROM n;""")
        resumen['tutores'] += 1

    # ---- Animales, titularidad, trámites y certificados ----
    consecutivos_sol = defaultdict(int)
    for t in tablas['temporal']:
        m = re.match(r'SOL-(\d{4})\d{4}-(\d+)', t['numero_solicitud'] or '')
        if m:
            consecutivos_sol[int(m.group(1))] = max(consecutivos_sol[int(m.group(1))], int(m.group(2)))
    consecutivos_cert = defaultdict(int)
    for c in tablas['certificados']:
        m = re.match(r'.*/EMP-(\d{5})/(\d+)/(\d{4})$', c['folio'] or '')
        if m:
            llave = (m.group(1), int(m.group(3)))
            consecutivos_cert[llave] = max(consecutivos_cert[llave], int(m.group(2)))

    usuarios_corregidos = set()
    fechas_tramite = {}
    pendientes = 0
    for m in sorted(mascotas, key=lambda x: int(x['id_mascota'])):
        p = personas[m['id_usuario']]
        especie = 'FELINO' if normal(m['especie']) in ('GATO', 'FELINO') else 'CANINO'
        sexo = 'HEMBRA' if normal(m['sexo']).startswith('H') else 'MACHO'
        raza, raza_texto = homologar_raza(m['raza'] or '', especie)
        color, color_texto = homologar_color(m['color'] or '')
        alta = fecha_utc(m['fecha_alta']) or datetime(2025, 9, 1, 16, 0)
        notas = [(m['caracteristicas'] or '').strip()]
        if raza_texto:
            notas.append(f'Raza registrada en el sistema anterior: {raza_texto}.')
        if color_texto:
            notas.append(f'Color registrado: {color_texto}.')
        caracteristicas = ' '.join(n for n in notas if n)[:1000] or None
        original = (tramites.get(m['id_mascota']) or {}).get('curp_mascota')
        if not anonimizar and original and original not in claves:
            clave = original.strip()[:60]
            claves.add(clave)
        else:
            clave = clave_animal({'paterno': p['paterno'], 'nombre': p['nombre']}, m['nombre'],
                                 especie, sexo, alta, claves)
        escribir(f"""WITH n AS (INSERT INTO animal (s_nombre, fn_especie_id, fn_raza_id, fn_color_id, fn_sexo_id,
    n_edad_meses, b_vacunado, b_esterilizado, s_caracteristicas, s_clave_animal, b_activo, d_fecha_creacion)
  VALUES ({sql((m['nombre'] or 'SIN NOMBRE').strip()[:120])},
    (SELECT pn_id FROM cat_especie WHERE s_clave = {sql(especie)}),
    (SELECT pn_id FROM cat_raza WHERE s_clave = {sql(raza)}),
    {('(SELECT pn_id FROM cat_color WHERE s_clave = ' + sql(color) + ')') if color else 'NULL'},
    (SELECT pn_id FROM cat_sexo WHERE s_clave = {sql(sexo)}),
    {sql(edad_en_meses(m['edad']))}, {sql(normal(m['vacunado']).startswith('S'))},
    {sql(normal(m['esterilizado']).startswith('S'))}, {sql(caracteristicas)}, {sql(clave)},
    {sql(m['activo'] != '0')}, {ts(alta)})
  RETURNING pn_id)
INSERT INTO mapa_animal SELECT {int(m['id_mascota'])}, pn_id FROM n;
INSERT INTO titularidad (fn_persona_id, fn_animal_id, d_fecha_inicio, b_vigente, d_fecha_creacion)
  SELECT p.id, a.id, {sql(alta.date().isoformat())}, TRUE, {ts(alta)}
  FROM mapa_persona p, mapa_animal a WHERE p.legado = {int(m['id_usuario'])} AND a.legado = {int(m['id_mascota'])};""")
        resumen['animales'] += 1

        tramite = tramites.get(m['id_mascota'])
        certificado = certificados.get(m['id_mascota'])
        if tramite:
            folio = tramite['numero_solicitud']
            fecha_folio = re.match(r'SOL-(\d{8})', folio)
            registro = datetime.strptime(fecha_folio.group(1), '%Y%m%d').replace(hour=16) if fecha_folio else alta
            registro = alta if alta.date() == registro.date() else registro
        else:
            # Animal sin trámite en el sistema anterior: queda como trámite por revisar.
            anio = alta.year
            consecutivos_sol[anio] += 1
            folio = f"SOL-{alta.strftime('%Y%m%d')}-{consecutivos_sol[anio]:05d}"
            registro = alta
            pendientes += 1

        emitido = fecha_utc(certificado['fecha_alta']) if certificado else None
        estatus = 'CERTIFICADO_EMITIDO' if certificado else ('EN_REVISION' if pendientes % 2 else 'REGISTRADA')
        resolucion = emitido
        escribir(f"""WITH n AS (INSERT INTO solicitud (s_folio_solicitud, fn_persona_id, fn_animal_id, fn_estatus_id,
    s_canal, d_fecha_registro, d_fecha_resolucion, fn_usuario_resolucion_id, s_aviso_privacidad_version,
    d_fecha_aceptacion_aviso, s_ip_origen, b_activo, d_fecha_creacion)
  SELECT {sql(folio)}, p.id, a.id, (SELECT pn_id FROM cat_estatus_solicitud WHERE s_clave = {sql(estatus)}),
    'PUBLICO', {ts(registro)}, {ts(resolucion)}, {'(SELECT id FROM personal_demo)' if resolucion else 'NULL'},
    (SELECT s_version FROM aviso_privacidad WHERE b_vigente ORDER BY pn_id DESC LIMIT 1),
    {ts(registro)}, '0.0.0.0', TRUE, {ts(registro)}
  FROM mapa_persona p, mapa_animal a WHERE p.legado = {int(m['id_usuario'])} AND a.legado = {int(m['id_mascota'])}
  RETURNING pn_id)
INSERT INTO mapa_solicitud SELECT {int(m['id_mascota'])}, pn_id FROM n;""")
        resumen['tramites'] += 1

        # Expediente: cada transición con su fecha, como la registraría el sistema.
        pasos = [(None, 'REGISTRADA', registro, None, None, 'Trámite registrado en el portal (sistema anterior)')]
        fechas_tramite[m['id_mascota']] = (registro, resolucion)
        if estatus != 'REGISTRADA':
            revision = registro + timedelta(hours=AZAR.randint(2, 30))
            if resolucion and revision >= resolucion:
                revision = registro + (resolucion - registro) / 4
            pasos.append(('REGISTRADA', 'EN_REVISION', revision, 'personal', None, 'Se toma para revisión'))
            if certificado:
                if m['id_usuario'] in correcciones and m['id_usuario'] not in usuarios_corregidos:
                    usuarios_corregidos.add(m['id_usuario'])
                    observada = revision + (resolucion - revision) / 3
                    pasos.append(('EN_REVISION', 'OBSERVADA', observada, 'personal', 'DATOS_INCOMPLETOS',
                                  'Se solicitó corrección de datos (sistema anterior)'))
                    pasos.append(('OBSERVADA', 'EN_REVISION', revision + (resolucion - revision) * 2 / 3, None,
                                  None, 'La persona corrigió sus datos'))
                pasos.append(('EN_REVISION', 'APROBADA', resolucion - timedelta(minutes=1), 'personal', None,
                              'Trámite aprobado (sistema anterior)'))
                pasos.append(('APROBADA', 'CERTIFICADO_EMITIDO', resolucion, 'personal', None,
                              f"Certificado {certificado['folio']} emitido"))
        for anterior, nuevo, cuando, quien, motivo, observacion in pasos:
            escribir(f"""INSERT INTO solicitud_historial (fn_solicitud_id, fn_estatus_anterior_id, fn_estatus_nuevo_id,
    fn_usuario_id, fn_motivo_rechazo_id, s_observacion, d_fecha)
  SELECT s.id, {('(SELECT pn_id FROM cat_estatus_solicitud WHERE s_clave = ' + sql(anterior) + ')') if anterior else 'NULL'},
    (SELECT pn_id FROM cat_estatus_solicitud WHERE s_clave = {sql(nuevo)}),
    {'(SELECT id FROM personal_demo)' if quien else 'NULL'},
    {('(SELECT pn_id FROM cat_motivo_rechazo WHERE s_clave = ' + sql(motivo) + ')') if motivo else 'NULL'},
    {sql(observacion)}, {ts(cuando)}
  FROM mapa_solicitud s WHERE s.legado = {int(m['id_mascota'])};""")

        if certificado:
            vigente = certificado['activo'] != '0'
            cancelado = fecha_utc(certificado['fecha_edit']) or emitido
            escribir(f"""INSERT INTO certificado (fn_solicitud_id, s_folio, d_fecha_emision, d_fecha_vigencia, fn_usuario_emisor_id,
    b_vigente, s_motivo_cancelacion, d_fecha_cancelacion, fn_usuario_cancelacion_id, d_fecha_creacion)
  SELECT s.id, {sql(certificado['folio'])}, {ts(emitido)}, NULL, (SELECT id FROM personal_demo),
    {sql(vigente)}, {sql(None if vigente else 'Cancelado en el sistema anterior')},
    {'NULL' if vigente else ts(cancelado)}, {'NULL' if vigente else '(SELECT id FROM personal_demo)'}, {ts(emitido)}
  FROM mapa_solicitud s WHERE s.legado = {int(m['id_mascota'])};""")
            resumen['certificados'] += 1
            resumen['certificados_cancelados'] += 0 if vigente else 1

    # ---- Avisos enviados por correo en el sistema anterior ----
    primera_mascota = {}
    for m in sorted(mascotas, key=lambda x: int(x['id_mascota'])):
        primera_mascota.setdefault(m['id_usuario'], m['id_mascota'])
    for n in sorted(tablas['notificaciones'], key=lambda x: int(x['id_notificacion'])):
        mascota = n['id_mascota'] or primera_mascota.get(n['id_usuario'])
        if not mascota or mascota not in fechas_tramite or n['id_usuario'] not in personas:
            continue
        destinatario = personas[n['id_usuario']]['correo'] or personas[n['id_usuario']]['telefono']
        if not destinatario:
            continue
        registro, resolucion = fechas_tramite[mascota]
        asunto = (n['asunto'] or '').strip() or 'Aviso del sistema anterior'
        cuando = resolucion if asunto.upper().startswith('CERTIFICADO') and resolucion else (
            registro + ((resolucion - registro) / 3 if resolucion else timedelta(days=1)))
        cuerpo = ' '.join(x for x in [(n['descripcion'] or '').strip(),
                                      f"Enlace: {n['enlace']}" if n['enlace'] else ''] if x) or None
        escribir(f"""INSERT INTO notificacion (fn_solicitud_id, s_canal, s_destinatario, s_asunto, s_cuerpo,
    s_estatus_envio, d_fecha_envio, n_intentos, d_fecha_creacion)
  SELECT s.id, 'AUTOMATICO', {sql(destinatario[:200])}, {sql(asunto[:300])}, {sql(cuerpo)},
    'ENVIADA', {ts(cuando)}, 1, {ts(cuando)}
  FROM mapa_solicitud s WHERE s.legado = {int(mascota)};""")
        resumen['avisos'] += 1

    # ---- Consecutivos de folio: los folios nuevos siguen después de los importados ----
    escribir('-- Consecutivos de folio para que los folios nuevos no choquen con los importados.')
    for anio, numero in sorted(consecutivos_sol.items()):
        escribir(f"""INSERT INTO folio_consecutivo (s_tipo, fn_municipio_id, n_anio, n_consecutivo)
  SELECT 'SOLICITUD', NULL, {anio}, {numero}
  WHERE NOT EXISTS (SELECT 1 FROM folio_consecutivo WHERE s_tipo = 'SOLICITUD' AND fn_municipio_id IS NULL AND n_anio = {anio});
UPDATE folio_consecutivo SET n_consecutivo = GREATEST(n_consecutivo, {numero})
  WHERE s_tipo = 'SOLICITUD' AND fn_municipio_id IS NULL AND n_anio = {anio};""")
    for (cve, anio), numero in sorted(consecutivos_cert.items()):
        escribir(f"""INSERT INTO folio_consecutivo (s_tipo, fn_municipio_id, n_anio, n_consecutivo)
  SELECT 'CERTIFICADO', m.pn_id, {anio}, {numero} FROM cat_municipio m WHERE m.s_cve_inegi = {sql(cve)}
  AND NOT EXISTS (SELECT 1 FROM folio_consecutivo f WHERE f.s_tipo = 'CERTIFICADO' AND f.fn_municipio_id = m.pn_id AND f.n_anio = {anio});
UPDATE folio_consecutivo f SET n_consecutivo = GREATEST(f.n_consecutivo, {numero})
  FROM cat_municipio m WHERE m.s_cve_inegi = {sql(cve)} AND f.s_tipo = 'CERTIFICADO' AND f.fn_municipio_id = m.pn_id AND f.n_anio = {anio};""")

    detalle = (f"Carga de datos de demostración migrados del sistema anterior "
               f"({'tutores anonimizados' if anonimizar else 'datos reales'}): "
               f"{resumen['tutores']} tutores, {resumen['animales']} animales, {resumen['tramites']} trámites, "
               f"{resumen['certificados']} certificados, {resumen['avisos']} avisos.")
    escribir(f"""INSERT INTO bitacora_auditoria (fn_usuario_id, s_entidad, s_accion, s_detalle)
  VALUES (NULL, 'carga_demo', 'CREAR', {sql(detalle)});

COMMIT;
""")
    return '\n'.join(salida).replace('{MODO}', modo), resumen, pendientes


def main():
    argumentos = [a for a in sys.argv[1:] if a != '--anonimizar']
    if len(argumentos) != 2:
        sys.exit(__doc__)
    anonimizar = '--anonimizar' in sys.argv[1:]
    sql_salida, resumen, pendientes = generar(leer_volcado(argumentos[0]), anonimizar)
    with open(argumentos[1], 'w', encoding='utf-8') as f:
        f.write(sql_salida)
    print(f"Tutores: {resumen['tutores']} | animales: {resumen['animales']} | trámites: {resumen['tramites']} "
          f"({pendientes} pendientes de revisión) | certificados: {resumen['certificados']} "
          f"({resumen['certificados_cancelados']} cancelado) | avisos: {resumen['avisos']}")
    print(f"Modo: {'anonimizado' if anonimizar else 'datos reales'} | script: {argumentos[1]}")


if __name__ == '__main__':
    main()
