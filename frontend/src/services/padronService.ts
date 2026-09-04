import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';
import type {
  ActualizarTutorRequest,
  Animal,
  Solicitud,
  Titularidad,
  Tutor,
  TutorDetalle,
} from '@/types/dominio';

/**
 * Padron de animales y tutores responsables.
 *
 * <p>Lo que se empadrona es el animal; la persona aparece como su tutor. Se
 * separan en dos servicios porque son dos formas distintas de llegar al mismo
 * dato: por el animal o por quien responde de el.
 */
export const tutorService = {
  /**
   * Busca tutores por nombre, apellidos o CURP.
   *
   * @returns los resultados y el mensaje que orienta cuando no hay ninguno
   */
  async buscar(
    texto: string,
    parametros: ParametrosPagina = {},
  ): Promise<{ resultados: Pagina<Tutor>; mensaje: string }> {
    const { data } = await api.get<ApiResponse<Pagina<Tutor>>>('/tutores', {
      params: { texto: texto || undefined, ...parametros },
    });
    return { resultados: data.data, mensaje: data.message };
  },

  /** Ficha completa; el acceso queda en bitacora. */
  async obtener(id: number): Promise<TutorDetalle> {
    const { data } = await api.get<ApiResponse<TutorDetalle>>(`/tutores/${id}`);
    return data.data;
  },

  /** Animales que tiene a su cargo ahora mismo. */
  async animales(id: number): Promise<Animal[]> {
    const { data } = await api.get<ApiResponse<Animal[]>>(`/tutores/${id}/animales`);
    return data.data;
  },

  /** Tramites del tutor. */
  async tramites(id: number, parametros: ParametrosPagina = {}): Promise<Pagina<Solicitud>> {
    const { data } = await api.get<ApiResponse<Pagina<Solicitud>>>(
      `/tutores/${id}/tramites`,
      { params: parametros },
    );
    return data.data;
  },

  /** Completa la CURP de quien se registro sin ella. */
  async completarCurp(
    id: number,
    curp: string,
  ): Promise<{ tutor: TutorDetalle; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<TutorDetalle>>(`/tutores/${id}/curp`, {
      curp,
    });
    return { tutor: data.data, mensaje: data.message };
  },

  /** Actualiza contacto y domicilio. */
  async actualizar(
    id: number,
    peticion: ActualizarTutorRequest,
  ): Promise<{ tutor: TutorDetalle; mensaje: string }> {
    const { data } = await api.put<ApiResponse<TutorDetalle>>(`/tutores/${id}`, peticion);
    return { tutor: data.data, mensaje: data.message };
  },
};

export const padronService = {
  /**
   * Busca animales por nombre, identificador o numero de chip.
   *
   * <p>La busqueda por chip sirve cuando aparece un animal perdido.
   */
  async buscar(
    texto: string,
    parametros: ParametrosPagina = {},
  ): Promise<{ resultados: Pagina<Animal>; mensaje: string }> {
    const { data } = await api.get<ApiResponse<Pagina<Animal>>>('/padron', {
      params: { texto: texto || undefined, ...parametros },
    });
    return { resultados: data.data, mensaje: data.message };
  },

  async obtener(id: number): Promise<Animal> {
    const { data } = await api.get<ApiResponse<Animal>>(`/padron/${id}`);
    return data.data;
  },

  /** Historial de tutores del animal. */
  async historialTitularidad(id: number): Promise<Titularidad[]> {
    const { data } = await api.get<ApiResponse<Titularidad[]>>(`/padron/${id}/titularidad`);
    return data.data;
  },

  /** Transfiere el animal a otro tutor. */
  async transferir(
    id: number,
    nuevoTutorId: number,
    motivo: string,
  ): Promise<{ titularidad: Titularidad; mensaje: string }> {
    const { data } = await api.post<ApiResponse<Titularidad>>(`/padron/${id}/transferir`, {
      nuevoTutorId,
      motivo,
    });
    return { titularidad: data.data, mensaje: data.message };
  },

  /** Registra la defuncion del animal. */
  async registrarDefuncion(
    id: number,
    fecha?: string,
    observacion?: string,
  ): Promise<{ animal: Animal; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<Animal>>(`/padron/${id}/defuncion`, {
      fecha,
      observacion,
    });
    return { animal: data.data, mensaje: data.message };
  },
};

export default padronService;
