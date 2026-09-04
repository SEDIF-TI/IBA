import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';
import type {
  MovimientoHistorial,
  ResolucionRequest,
  Seguimiento,
  Solicitud,
} from '@/types/dominio';

/** Filtros de la bandeja de tramites. */
export interface FiltrosBandeja extends ParametrosPagina {
  estatusId?: number;
  municipioId?: number;
  jornadaId?: number;
  desde?: string;
  hasta?: string;
}

/** Alta de un tramite desde el portal. */
export interface RegistroPublicoRequest {
  tutor: {
    curp?: string;
    nombre: string;
    apellidoPaterno: string;
    apellidoMaterno?: string;
    correo?: string;
    telefono?: string;
    telefonoWhatsapp?: string;
    municipioId: number;
    calle?: string;
    numeroExterior?: string;
    numeroInterior?: string;
    colonia?: string;
    codigoPostal?: string;
  };
  animal: {
    nombre: string;
    especieId: number;
    razaId?: number;
    colorId?: number;
    sexoId: number;
    tamanoId?: number;
    edadAnios?: number;
    edadMeses?: number;
    vacunado?: boolean;
    esterilizado?: boolean;
    caracteristicas?: string;
    metodoIdentificacionId?: number;
    identificador?: string;
  };
  aceptaAviso: boolean;
  avisoVersion: string;
  /** Jornada para la que se adelanta el registro, si aplica. */
  jornadaId?: number;
}

/** Respuesta del alta publica. */
export interface RegistroRespuesta {
  folio: string;
  token: string;
  estatus: string;
  mensaje: string;
}

/** Tramites: alta publica, bandeja y resoluciones. */
export const solicitudService = {
  /** Alta desde el portal, sin autenticacion. */
  async registrarPublico(peticion: RegistroPublicoRequest): Promise<RegistroRespuesta> {
    const { data } = await api.post<ApiResponse<RegistroRespuesta>>(
      '/publico/solicitud',
      peticion,
    );
    return data.data;
  },

  /**
   * Consulta del avance del tramite.
   *
   * <p>Exige folio Y CURP: con la CURP sola, quien conociera la de un tercero
   * podria ver sus datos.
   */
  async consultarPublico(folio: string, curp: string): Promise<Seguimiento> {
    const { data } = await api.get<ApiResponse<Seguimiento>>('/publico/consulta', {
      params: { folio, curp },
    });
    return data.data;
  },

  /**
   * Captura un tramite en ventanilla.
   *
   * <p>Para cuando la persona llega sin haberse registrado: no tiene internet,
   * o llego a la jornada sin precargar.
   */
  async capturarEnVentanilla(
    peticion: RegistroPublicoRequest,
  ): Promise<{ solicitud: Solicitud; mensaje: string }> {
    const { data } = await api.post<ApiResponse<Solicitud>>('/solicitudes', peticion);
    return { solicitud: data.data, mensaje: data.message };
  },

  /** Bandeja de trabajo del panel. */
  async listar(filtros: FiltrosBandeja = {}): Promise<Pagina<Solicitud>> {
    const { data } = await api.get<ApiResponse<Pagina<Solicitud>>>('/solicitudes', {
      params: filtros,
    });
    return data.data;
  },

  async obtener(id: number): Promise<Solicitud> {
    const { data } = await api.get<ApiResponse<Solicitud>>(`/solicitudes/${id}`);
    return data.data;
  },

  /** Expediente: cada movimiento con su responsable. */
  async historial(id: number): Promise<MovimientoHistorial[]> {
    const { data } = await api.get<ApiResponse<MovimientoHistorial[]>>(
      `/solicitudes/${id}/historial`,
    );
    return data.data;
  },

  /** Toma el tramite para revisarlo. */
  async tomarParaRevision(id: number): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(`/solicitudes/${id}/revisar`, {});
    return data.data;
  },

  async aprobar(id: number, peticion: ResolucionRequest): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(
      `/solicitudes/${id}/aprobar`,
      peticion,
    );
    return data.data;
  },

  /** Devuelve el tramite para que la persona corrija. Exige motivo. */
  async observar(id: number, peticion: ResolucionRequest): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(
      `/solicitudes/${id}/observar`,
      peticion,
    );
    return data.data;
  },

  /** Rechaza el tramite. Exige motivo. */
  async rechazar(id: number, peticion: ResolucionRequest): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(
      `/solicitudes/${id}/rechazar`,
      peticion,
    );
    return data.data;
  },

  /** Registra que la persona recogio su documentacion. Cierra el tramite. */
  async registrarEntrega(id: number, peticion: ResolucionRequest): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(
      `/solicitudes/${id}/entregar`,
      peticion,
    );
    return data.data;
  },

  /** Cancela el tramite. Exige motivo. */
  async cancelar(id: number, peticion: ResolucionRequest): Promise<Solicitud> {
    const { data } = await api.patch<ApiResponse<Solicitud>>(
      `/solicitudes/${id}/cancelar`,
      peticion,
    );
    return data.data;
  },
};

export default solicitudService;
