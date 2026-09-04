import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';
import type { Notificacion, ResumenBandeja } from '@/types/dominio';

/**
 * Avisos del tramite y bandeja de contacto.
 *
 * <p>La bandeja reune a quienes hay que llamar: las personas que solo dejaron
 * telefono. Sin ella esos avisos no ocurririan y nadie se enteraria de que hay
 * gente esperando respuesta.
 */
export const notificacionService = {
  /** Contactos pendientes, del mas antiguo al mas reciente. */
  async bandeja(parametros: ParametrosPagina = {}): Promise<Pagina<Notificacion>> {
    const { data } = await api.get<ApiResponse<Pagina<Notificacion>>>(
      '/notificaciones/bandeja',
      { params: parametros },
    );
    return data.data;
  },

  /** Cuantos contactos quedan por atender, para el indicador del panel. */
  async pendientes(): Promise<ResumenBandeja> {
    const { data } = await api.get<ApiResponse<ResumenBandeja>>(
      '/notificaciones/bandeja/pendientes',
    );
    return data.data;
  },

  /** Avisos de un tramite. */
  async porSolicitud(solicitudId: number): Promise<Notificacion[]> {
    const { data } = await api.get<ApiResponse<Notificacion[]>>(
      `/notificaciones/solicitud/${solicitudId}`,
    );
    return data.data;
  },

  /**
   * Registra el resultado de una llamada.
   *
   * @param observacion que se le dijo o por que no se pudo; es obligatoria
   */
  async registrarContacto(
    id: number,
    logrado: boolean,
    observacion: string,
  ): Promise<{ notificacion: Notificacion; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<Notificacion>>(
      `/notificaciones/${id}/contacto`,
      { logrado, observacion },
    );
    return { notificacion: data.data, mensaje: data.message };
  },

  /** Reintenta los avisos por correo que fallaron. */
  async reintentar(): Promise<{ enviados: number; mensaje: string }> {
    const { data } = await api.post<ApiResponse<number>>('/notificaciones/reintentar');
    return { enviados: data.data, mensaje: data.message };
  },
};

export default notificacionService;
