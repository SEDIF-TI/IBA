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
