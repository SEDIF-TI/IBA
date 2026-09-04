/** Tipos del dominio: lo que el sistema registra y expide. */

/** Registro de un catalogo administrable. */
export interface Catalogo {
  id: number;
  clave: string;
  nombre: string;
  descripcion?: string;
  orden: number;
  activo: boolean;
}

/** Municipio, con su clave geoestadistica. */
export interface Municipio extends Catalogo {
  claveInegi: string;
}

/** Raza, siempre subordinada a una especie. */
export interface Raza extends Catalogo {
  especieId: number;
  especieNombre: string;
}

/** Punto de entrega de la documentacion. */
export interface Sede extends Catalogo {
  domicilio: string;
  municipioId: number;
  municipioNombre: string;
  horario?: string;
  telefono?: string;
  correo?: string;
}

/** Datos para dar de alta o editar un catalogo. */
export interface CatalogoRequest {
  clave: string;
  nombre: string;
  descripcion?: string;
  orden?: number;
  activo?: boolean;
}

/** Situaciones por las que pasa un tramite. */
export type ClaveEstatus =
  | 'REGISTRADA'
  | 'EN_REVISION'
  | 'OBSERVADA'
  | 'APROBADA'
  | 'CERTIFICADO_EMITIDO'
  | 'ENTREGADA'
  | 'RECHAZADA'
  | 'CANCELADA';

/** Via por la que entro el tramite. */
export type CanalSolicitud = 'PUBLICO' | 'JORNADA' | 'VENTANILLA';

/** Tramite tal como se ve en la bandeja. */
export interface Solicitud {
  id: number;
  folio: string;
  estatusClave: ClaveEstatus;
  estatusNombre: string;
  estatusColor?: string;
  canal: CanalSolicitud;
  tutorNombre: string;
  /** Enmascarada: en un listado no hay motivo para verla completa. */
  tutorCurpEnmascarada?: string;
  tutorMunicipio?: string;
  animalNombre: string;
  animalEspecie: string;
  revisionReforzada: boolean;
  motivoRevisionReforzada?: string;
  fechaRegistro: string;
  fechaResolucion?: string;
}

/** Movimiento del expediente. */
export interface MovimientoHistorial {
  id: number;
  estatusAnterior?: string;
  estatusNuevo: string;
  usuario: string;
  motivo?: string;
  observacion?: string;
  fecha: string;
}

/** Resolucion que toma el personal sobre un tramite. */
export interface ResolucionRequest {
  motivoId?: number;
  observacion?: string;
}

/** Situacion del tramite en la consulta publica. */
export interface Seguimiento {
  folio: string;
  estatusClave: ClaveEstatus;
  estatusNombre: string;
  estatusDescripcion?: string;
  estatusColor?: string;
  animalNombre: string;
  requiereCorreccion: boolean;
  motivo?: string;
  observacion?: string;
  fechaRegistro: string;
  fechaResolucion?: string;
}

/** Documento adjunto a un tramite. */
export interface Documento {
  id: number;
  tipoClave: string;
  tipoNombre: string;
  nombreOriginal: string;
  mime: string;
  tamanoBytes: number;
  /** Sin valor mientras nadie lo revisa. */
  legible?: boolean;
  observacion?: string;
  fechaCarga: string;
}

/** Certificado emitido tras aprobar un tramite. */
export interface Certificado {
  id: number;
  folio: string;
  codigoVerificacion: string;
  folioSolicitud: string;
  animalNombre: string;
  tutorNombre: string;
  fechaEmision: string;
  fechaVigencia?: string;
  vigente: boolean;
  motivoCancelacion?: string;
  entregado: boolean;
  firmado: boolean;
}

/**
 * Respuesta del verificador publico, destino del codigo QR.
 *
 * <p>Deliberadamente sin datos del tutor: quien escanea el documento puede ser
 * cualquiera y no tiene por que enterarse de quien responde por el animal.
 */
export interface Verificacion {
  valido: boolean;
  folio?: string;
  animalNombre?: string;
  animalEspecie?: string;
  animalRaza?: string;
  animalSexo?: string;
  animalColor?: string;
  animalEdad?: string;
  municipio?: string;
  fechaEmision?: string;
  fechaVigencia?: string;
  mensaje: string;
}

/** Jornada municipal de empadronamiento. */
export interface Jornada {
  id: number;
  nombre: string;
  descripcion?: string;
  municipioId?: number;
  municipio?: string;
  domicilioSede: string;
  fechaInicio: string;
  fechaFin: string;
  horario?: string;
  cupo?: number;
  publicada: boolean;
  fechaPublicacion?: string;
  responsable?: string;
  enCurso: boolean;
}

/** Jornada tal como la ve la ciudadania en el portal. */
export interface JornadaPublica {
  id: number;
  nombre: string;
  descripcion?: string;
  municipio?: string;
  domicilioSede: string;
  fechaInicio: string;
  fechaFin: string;
  horario?: string;
}

/** Datos para dar de alta o editar una jornada. */
export interface JornadaRequest {
  nombre: string;
  descripcion?: string;
  municipioId: number;
  domicilioSede: string;
  fechaInicio: string;
  fechaFin: string;
  horario?: string;
  cupo?: number;
  usuarioResponsableId?: number;
}

/** Avance de una jornada durante el evento. */
export interface ResumenJornada {
  jornadaId: number;
  nombre: string;
  municipio: string;
  precargadas: number;
  atendidas: number;
  pendientes: number;
  cupo?: number;
  lugaresLibres?: number;
}

/** Aviso del tramite, tal como se ve en la bandeja de contacto. */
export interface Notificacion {
  id: number;
  folioSolicitud: string;
  tutorNombre: string;
  destinatario: string;
  canal: 'AUTOMATICO' | 'MANUAL';
  asunto?: string;
  cuerpo?: string;
  estatus: 'PENDIENTE' | 'ENVIADA' | 'FALLIDA' | 'CONTACTADA' | 'NO_CONTACTADA';
  intentos: number;
  error?: string;
  observacion?: string;
  atendioUsuario?: string;
  fechaCreacion: string;
  fechaContacto?: string;
}

/** Situacion de la bandeja de contacto. */
export interface ResumenBandeja {
  pendientes: number;
  envioHabilitado: boolean;
}

/** Tutor responsable, en el listado. */
export interface Tutor {
  id: number;
  nombreCompleto: string;
  curpEnmascarada?: string;
  identidadValidada: boolean;
  correo?: string;
  telefono?: string;
  municipio?: string;
  animalesACargo: number;
}

/**
 * Ficha completa del tutor.
 *
 * <p>Aqui la CURP va completa, para cotejarla contra el documento. Por eso
 * abrir la ficha queda registrado en bitacora.
 */
export interface TutorDetalle {
  id: number;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  nombreCompleto: string;
  curp?: string;
  identidadValidada: boolean;
  fechaNacimiento?: string;
  sexoCurp?: string;
  entidadNacimiento?: string;
  entidadNacimientoNombre?: string;
  correo?: string;
  telefono?: string;
  telefonoWhatsapp?: string;
  municipioId?: number;
  municipio?: string;
  calle?: string;
  numeroExterior?: string;
  numeroInterior?: string;
  colonia?: string;
  codigoPostal?: string;
  animalesACargo: number;
}

/** Animal empadronado. */
export interface Animal {
  id: number;
  nombre: string;
  claveAnimal?: string;
  especie?: string;
  raza?: string;
  sexo?: string;
  color?: string;
  tamano?: string;
  edadMeses?: number;
  edadDescriptiva?: string;
  vacunado?: boolean;
  esterilizado?: boolean;
  caracteristicas?: string;
  metodoIdentificacion?: string;
  identificador?: string;
  finado?: boolean;
  fechaDefuncion?: string;
  tutorNombre?: string;
  tutorId?: number;
}

/** Un tramo del historial de tutores de un animal. */
export interface Titularidad {
  id: number;
  tutorId: number;
  tutorNombre: string;
  fechaInicio: string;
  fechaFin?: string;
  motivoBaja?: string;
  vigente: boolean;
}

/** Datos editables del tutor. */
export interface ActualizarTutorRequest {
  correo?: string;
  telefono?: string;
  telefonoWhatsapp?: string;
  municipioId?: number;
  calle?: string;
  numeroExterior?: string;
  numeroInterior?: string;
  colonia?: string;
  codigoPostal?: string;
}
