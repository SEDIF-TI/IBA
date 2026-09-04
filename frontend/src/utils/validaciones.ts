/**
 * Validaciones de los campos que la persona captura.
 *
 * <p>El backend valida de nuevo todo esto: es él quien protege la base de
 * datos. Estas comprobaciones existen para avisar mientras se escribe, en vez
 * de dejar que la persona llene el formulario completo y descubra el error al
 * enviarlo.
 *
 * <p>Cada función devuelve el mensaje de error, o `null` si el valor es
 * correcto.
 */

/** Resultado de comprobar un campo. */
export interface Validacion {
  /** Mensaje de error, o null si está bien. */
  error: string | null;
  /** Aviso que se muestra cuando el valor es correcto. */
  confirmacion?: string;
}

const CORRECTO: Validacion = { error: null };

// ---------------------------------------------------------------------------
// CURP
// ---------------------------------------------------------------------------

/** Estructura de la CURP, según RENAPO. */
const FORMATO_CURP = /^[A-Z][AEIOUX][A-Z]{2}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[HM](AS|BC|BS|CC|CH|CL|CM|CS|DF|DG|GR|GT|HG|JC|MC|MN|MS|NE|NL|NT|OC|PL|QR|QT|SL|SP|SR|TC|TL|TS|VZ|YN|ZS)[BCDFGHJKLMNÑPQRSTVWXYZ]{3}[A-Z\d]\d$/;

/** Alfabeto con el que se calcula el dígito verificador. */
const ALFABETO_CURP = '0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZ';

/**
 * Palabras que RENAPO sustituye por XXXX en las primeras cuatro posiciones.
 *
 * <p>Cuando las iniciales del nombre forman una de estas, la segunda letra se
 * reemplaza por X.
 */
const PALABRAS_INCONVENIENTES = [
  'BACA', 'BAKA', 'BUEI', 'BUEY', 'CACA', 'CACO', 'CAGA', 'CAGO', 'CAKA', 'CAKO',
  'COGE', 'COGI', 'COJA', 'COJE', 'COJI', 'COJO', 'COLA', 'CULO', 'FALO', 'FETO',
  'GETA', 'GUEI', 'GUEY', 'JETA', 'JOTO', 'KACA', 'KACO', 'KAGA', 'KAGO', 'KAKA',
  'KAKO', 'KOGE', 'KOGI', 'KOJA', 'KOJE', 'KOJI', 'KOJO', 'KOLA', 'KULO', 'LILO',
  'LOCA', 'LOCO', 'LOKA', 'LOKO', 'MAME', 'MAMO', 'MEAR', 'MEAS', 'MEON', 'MIAR',
  'MION', 'MOCO', 'MOKO', 'MULA', 'MULO', 'NACA', 'NACO', 'PEDA', 'PEDO', 'PENE',
  'PIPI', 'PITO', 'POPO', 'PUTA', 'PUTO', 'QULO', 'RATA', 'ROBA', 'ROBE', 'ROBO',
  'RUIN', 'SENO', 'TETA', 'VACA', 'VAGA', 'VAGO', 'VAKA', 'VUEI', 'VUEY', 'WUEI',
  'WUEY',
];

/**
 * Comprueba la CURP: estructura, fecha, entidad y dígito verificador.
 *
 * <p>El dígito verificador es lo que distingue una CURP real de una inventada
 * con la forma correcta. Sin comprobarlo, cualquier cadena bien armada pasaría.
 *
 * @param curp valor capturado
 * @param obligatoria si vacío debe reportarse como error
 */
export function validarCurp(curp: string, obligatoria = false): Validacion {
  const valor = curp.trim().toUpperCase();

  if (!valor) {
    return obligatoria
      ? { error: 'Escribe la CURP' }
      : CORRECTO;
  }

  if (valor.length < 18) {
    return { error: `Faltan ${18 - valor.length} caracteres: la CURP tiene 18` };
  }
  if (valor.length > 18) {
    return { error: `Sobran ${valor.length - 18} caracteres: la CURP tiene 18` };
  }

  if (!FORMATO_CURP.test(valor)) {
    return { error: describirErrorDeEstructura(valor) };
  }

  if (!digitoVerificadorCorrecto(valor)) {
    return {
      error:
        'La CURP no es válida: el último dígito no corresponde. ' +
        'Revísala contra tu documento oficial.',
    };
  }

  const nacimiento = fechaDeNacimiento(valor);
  if (!nacimiento) {
    return { error: 'La fecha de nacimiento que contiene la CURP no existe' };
  }
  if (nacimiento > new Date()) {
    return { error: 'La CURP indica una fecha de nacimiento futura' };
  }

  return {
    error: null,
    confirmacion: `CURP válida. Nacimiento: ${nacimiento.toLocaleDateString('es-MX', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })}`,
  };
}

/** Explica en qué posición falla la estructura, en lugar de decir solo "inválida". */
function describirErrorDeEstructura(curp: string): string {
  if (!/^[A-Z]{4}/.test(curp)) {
    return 'Las primeras 4 posiciones deben ser letras (iniciales de tus apellidos y nombre)';
  }
  if (!/^[A-Z]{4}\d{6}/.test(curp)) {
    return 'Las posiciones 5 a 10 deben ser tu fecha de nacimiento en formato AAMMDD';
  }
  const mes = curp.slice(6, 8);
  if (mes < '01' || mes > '12') {
    return `El mes de nacimiento "${mes}" no existe: debe ir de 01 a 12`;
  }
  const dia = curp.slice(8, 10);
  if (dia < '01' || dia > '31') {
    return `El día de nacimiento "${dia}" no existe: debe ir de 01 a 31`;
  }
  if (!/^[HM]$/.test(curp.charAt(10))) {
    return 'La posición 11 indica el sexo y debe ser H o M';
  }
  if (!/^(AS|BC|BS|CC|CH|CL|CM|CS|DF|DG|GR|GT|HG|JC|MC|MN|MS|NE|NL|NT|OC|PL|QR|QT|SL|SP|SR|TC|TL|TS|VZ|YN|ZS)$/
    .test(curp.slice(11, 13))) {
    return `"${curp.slice(11, 13)}" no es una clave de entidad válida (posiciones 12 y 13)`;
  }
  if (!/^[BCDFGHJKLMNÑPQRSTVWXYZ]{3}$/.test(curp.slice(13, 16))) {
    return 'Las posiciones 14 a 16 deben ser consonantes';
  }
  return 'La CURP no tiene la estructura correcta. Revísala contra tu documento oficial.';
}

/** Comprueba el dígito verificador con el algoritmo de RENAPO. */
function digitoVerificadorCorrecto(curp: string): boolean {
  let suma = 0;
  for (let i = 0; i < 17; i++) {
    const valor = ALFABETO_CURP.indexOf(curp.charAt(i));
    if (valor < 0) return false;
    suma += valor * (18 - i);
  }
  const esperado = (10 - (suma % 10)) % 10;
  return esperado === Number(curp.charAt(17));
}

/**
 * Fecha de nacimiento contenida en la CURP.
 *
 * <p>El siglo se deduce de la posición 17: dígito para el siglo XX, letra para
 * el XXI.
 */
function fechaDeNacimiento(curp: string): Date | null {
  const anio = Number(curp.slice(4, 6));
  const mes = Number(curp.slice(6, 8));
  const dia = Number(curp.slice(8, 10));
  const siglo = /\d/.test(curp.charAt(16)) ? 1900 : 2000;

  const fecha = new Date(siglo + anio, mes - 1, dia);

  // Si la fecha no existe, JavaScript la corre al mes siguiente.
  if (fecha.getDate() !== dia || fecha.getMonth() !== mes - 1) {
    return null;
  }
  return fecha;
}

/**
 * Comprueba que las iniciales de la CURP correspondan con el nombre capturado.
 *
 * <p>Es un aviso, no un bloqueo: hay apellidos compuestos y casos que la regla
 * general de RENAPO no cubre. Rechazar de plano dejaría fuera a personas con
 * nombres legítimos.
 */
export function curpCoincideConNombre(
  curp: string,
  nombre: string,
  apellidoPaterno: string,
  apellidoMaterno: string,
): boolean {
  const valor = curp.trim().toUpperCase();
  if (valor.length !== 18 || !nombre.trim() || !apellidoPaterno.trim()) {
    return true;
  }

  const limpiar = (texto: string) =>
    texto
      .trim()
      .toUpperCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^A-ZÑ]/g, '');

  const paterno = limpiar(apellidoPaterno);
  const materno = limpiar(apellidoMaterno);

  // RENAPO omite María y José cuando hay un segundo nombre.
  const partes = nombre.trim().split(/\s+/);
  const primero = limpiar(partes[0] ?? '');
  const pila = limpiar(
    partes.length > 1 && ['MARIA', 'JOSE', 'MA', 'J'].includes(primero)
      ? partes[1] ?? ''
      : partes[0] ?? '',
  );

  if (!paterno || !pila) return true;

  const vocalInterna = paterno.slice(1).match(/[AEIOU]/)?.[0] ?? 'X';
  let esperada = paterno.charAt(0) + vocalInterna + (materno.charAt(0) || 'X') + pila.charAt(0);

  // Si las iniciales forman una palabra inconveniente, RENAPO cambia la segunda.
  if (PALABRAS_INCONVENIENTES.includes(esperada)) {
    esperada = esperada.charAt(0) + 'X' + esperada.slice(2);
  }

  return valor.slice(0, 4) === esperada;
}

// ---------------------------------------------------------------------------
// Correo electrónico
// ---------------------------------------------------------------------------

/**
 * Estructura de un correo.
 *
 * <p>Deliberadamente no se intenta cubrir toda la norma: se comprueba lo que
 * de verdad importa, que haya un nombre, una arroba, un dominio y una
 * extensión. Una expresión que cubra la norma completa rechaza direcciones
 * válidas y acepta cosas que ningún servidor entrega.
 */
const FORMATO_CORREO = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/;

/** Dominios que suelen escribirse mal. */
const CORRECCIONES_COMUNES: Record<string, string> = {
  'gmail.co': 'gmail.com',
  'gmail.con': 'gmail.com',
  'gmial.com': 'gmail.com',
  'gmai.com': 'gmail.com',
  'hotmail.co': 'hotmail.com',
  'hotmail.con': 'hotmail.com',
  'hotmial.com': 'hotmail.com',
  'outlok.com': 'outlook.com',
  'yahoo.co': 'yahoo.com',
  'yaho.com': 'yahoo.com',
};

/**
 * Comprueba un correo y sugiere una corrección si el dominio parece un error
 * de dedo frecuente.
 *
 * @param correo valor capturado
 * @param obligatorio si vacío debe reportarse como error
 */
export function validarCorreo(correo: string, obligatorio = false): Validacion {
  const valor = correo.trim();

  if (!valor) {
    return obligatorio ? { error: 'Escribe tu correo electrónico' } : CORRECTO;
  }

  if (!valor.includes('@')) {
    return { error: 'Falta la arroba (@). Un correo se ve así: nombre@dominio.com' };
  }

  const partes = valor.split('@');
  if (partes.length > 2) {
    return { error: 'El correo tiene más de una arroba' };
  }

  const [usuario, dominio] = partes;

  if (!usuario) {
    return { error: 'Falta el nombre antes de la arroba' };
  }
  if (!dominio) {
    return { error: 'Falta el dominio después de la arroba, por ejemplo gmail.com' };
  }
  if (!dominio.includes('.')) {
    return { error: `Al dominio "${dominio}" le falta la terminación, por ejemplo .com` };
  }
  if (!FORMATO_CORREO.test(valor)) {
    return { error: 'El correo no tiene una estructura válida' };
  }

  const sugerencia = CORRECCIONES_COMUNES[dominio.toLowerCase()];
  if (sugerencia) {
    return {
      error: null,
      confirmacion: `¿Quisiste escribir ${usuario}@${sugerencia}?`,
    };
  }

  return { error: null, confirmacion: 'Correo válido' };
}

// ---------------------------------------------------------------------------
// Teléfono
// ---------------------------------------------------------------------------

/**
 * Comprueba un teléfono de diez dígitos.
 *
 * <p>Se admite escribirlo con espacios, guiones o paréntesis, como viene en una
 * tarjeta: eso se limpia antes de guardar. Lo que no se admite son letras.
 */
export function validarTelefono(telefono: string, obligatorio = false): Validacion {
  const valor = telefono.trim();

  if (!valor) {
    return obligatorio ? { error: 'Escribe un teléfono de contacto' } : CORRECTO;
  }

  if (/[a-zA-ZñÑ]/.test(valor)) {
    return { error: 'El teléfono solo lleva números' };
  }

  const digitos = valor.replace(/\D/g, '');

  if (digitos.length < 10) {
    return { error: `Faltan ${10 - digitos.length} dígitos: el teléfono lleva 10` };
  }
  if (digitos.length > 10) {
    return { error: `Sobran ${digitos.length - 10} dígitos: el teléfono lleva 10` };
  }
  if (/^(\d)\1{9}$/.test(digitos)) {
    return { error: 'Ese número no parece real: son diez dígitos iguales' };
  }

  return { error: null, confirmacion: 'Teléfono válido' };
}

/** Deja solo los dígitos de un teléfono. */
export function soloDigitos(valor: string, maximo = 10): string {
  return valor.replace(/\D/g, '').slice(0, maximo);
}

// ---------------------------------------------------------------------------
// Otros campos
// ---------------------------------------------------------------------------

/** Comprueba un código postal de cinco dígitos. */
export function validarCodigoPostal(codigo: string): Validacion {
  const valor = codigo.trim();
  if (!valor) return CORRECTO;

  if (/\D/.test(valor)) {
    return { error: 'El código postal solo lleva números' };
  }
  if (valor.length !== 5) {
    return { error: 'El código postal tiene 5 dígitos' };
  }
  return CORRECTO;
}

/**
 * Comprueba un nombre o apellido.
 *
 * <p>Se admiten acentos, la eñe, apóstrofos y guiones: hay apellidos como
 * D'Angelo o Pérez-Reverte, y negarlos dejaría fuera a personas reales.
 */
export function validarNombre(nombre: string, etiqueta: string): Validacion {
  const valor = nombre.trim();

  if (!valor) {
    return { error: `Escribe ${etiqueta}` };
  }
  if (valor.length < 2) {
    return { error: `${etiqueta} es demasiado corto` };
  }
  if (/\d/.test(valor)) {
    return { error: `${etiqueta} no lleva números` };
  }
  if (!/^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ'\- .]+$/.test(valor)) {
    return { error: `${etiqueta} tiene caracteres que no corresponden a un nombre` };
  }
  return CORRECTO;
}

/** Comprueba la edad del animal en años y meses. */
export function validarEdad(anios: string, meses: string): Validacion {
  if (!anios && !meses) return CORRECTO;

  const a = anios ? Number(anios) : 0;
  const m = meses ? Number(meses) : 0;

  if (anios && (!Number.isInteger(a) || a < 0)) {
    return { error: 'Los años deben ser un número entero' };
  }
  if (meses && (!Number.isInteger(m) || m < 0)) {
    return { error: 'Los meses deben ser un número entero' };
  }
  if (m > 11) {
    return { error: 'Los meses van de 0 a 11: si es un año o más, ponlo en años' };
  }
  if (a > 30) {
    return { error: 'Revisa la edad: 30 años es mucho para un perro o un gato' };
  }
  if (a === 0 && m === 0) {
    return { error: 'Indica al menos los meses que tiene' };
  }
  return CORRECTO;
}
