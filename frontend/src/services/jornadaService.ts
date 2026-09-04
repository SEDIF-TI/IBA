import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';
import type {
  Jornada,
  JornadaPublica,
  JornadaRequest,
  ResumenJornada,
  Solicitud,
} from '@/types/dominio';

/**
 * Jornadas municipales.
 *
 * <p>Incluye la busqueda del dia del evento, que es la operacion que el
 * personal usa con la gente formada enfrente.
 */
export const jornadaService = {
  async listar(): Promise<Jornada[]> {
    const { data } = await api.get<ApiResponse<Jornada[]>>('/jornadas');
    return data.data;
  },

  /** Jornadas que se estan celebrando hoy. */
  async enCurso(): Promise<Jornada[]> {
    const { data } = await api.get<ApiResponse<Jornada[]>>('/jornadas/en-curso');
    return data.data;
  },

  async obtener(id: number): Promise<Jornada> {
    const { data } = await api.get<ApiResponse<Jornada>>(`/jornadas/${id}`);
    return data.data;
  },

  /** Avance de la jornada, para seguirla durante el evento. */
  async resumen(id: number): Promise<ResumenJornada> {
    const { data } = await api.get<ApiResponse<ResumenJornada>>(`/jornadas/${id}/resumen`);
    return data.data;
  },

  /**
   * Busca a una persona entre las precargas de la jornada.
   *
   * <p>Admite CURP completa, apellidos o nombre, e ignora acentos.
   *
   * @returns los resultados y el mensaje que orienta cuando no hay ninguno
   */
  async buscar(
    id: number,
    texto: string,
    parametros: ParametrosPagina = {},
  ): Promise<{ resultados: Pagina<Solicitud>; mensaje: string }> {
    const { data } = await api.get<ApiResponse<Pagina<Solicitud>>>(
      `/jornadas/${id}/buscar`,
      { params: { texto: texto || undefined, ...parametros } },
    );
    return { resultados: data.data, mensaje: data.message };
  },

  async crear(peticion: JornadaRequest): Promise<Jornada> {
    const { data } = await api.post<ApiResponse<Jornada>>('/jornadas', peticion);
    return data.data;
  },

  async actualizar(id: number, peticion: JornadaRequest): Promise<Jornada> {
    const { data } = await api.put<ApiResponse<Jornada>>(`/jornadas/${id}`, peticion);
    return data.data;
  },

  /** Publica la jornada para que la ciudadania la vea y precargue. */
  async publicar(id: number): Promise<Jornada> {
    const { data } = await api.patch<ApiResponse<Jornada>>(`/jornadas/${id}/publicar`);
    return data.data;
  },

  /** Retira la jornada del portal; las precargas se conservan. */
  async despublicar(id: number, motivo: string): Promise<Jornada> {
    const { data } = await api.patch<ApiResponse<Jornada>>(
      `/jornadas/${id}/despublicar`,
      { motivo },
    );
    return data.data;
  },

  /** Calendario publico de jornadas, sin sesion. */
  async calendarioPublico(): Promise<JornadaPublica[]> {
    const { data } = await api.get<ApiResponse<JornadaPublica[]>>('/publico/jornadas');
    return data.data;
  },
};

export default jornadaService;
