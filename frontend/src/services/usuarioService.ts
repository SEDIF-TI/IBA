import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';

/** Cuenta del personal. */
export interface Usuario {
  id: number;
  usuario: string;
  nombreCompleto: string;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno: string | null;
  correo: string;
  telefono: string | null;
  sedeId: number | null;
  sedeNombre: string | null;
  roles: RolResumen[];
  activo: boolean;
  cambioPasswordRequerido: boolean;
  bloqueada: boolean;
  ultimoAcceso: string | null;
}

export interface RolResumen {
  id: number;
  clave: string;
  nombre: string;
}

export interface GuardarUsuarioRequest {
  usuario: string;
  nombre: string;
  apellidoPaterno: string;
  apellidoMaterno?: string;
  correo: string;
  telefono?: string;
  sedeId?: number | null;
  rolesIds: number[];
}

/** Alta o reinicio: trae la contrasena que hay que entregar. */
export interface AltaRespuesta {
  usuario: Usuario;
  passwordTemporal: string;
}

/**
 * Cuentas del personal.
 *
 * <p>Ver la lista y administrarla son permisos distintos: el IBA necesita
 * saber quien tiene acceso, pero dar de alta es de informatica.
 */
export const usuarioService = {
  async buscar(
    texto: string,
    parametros: ParametrosPagina & { soloActivos?: boolean } = {},
  ): Promise<{ pagina: Pagina<Usuario>; mensaje: string }> {
    const { data } = await api.get<ApiResponse<Pagina<Usuario>>>('/usuarios', {
      params: { texto: texto || undefined, ...parametros },
    });
    return { pagina: data.data, mensaje: data.message };
  },

  async obtener(id: number): Promise<Usuario> {
    const { data } = await api.get<ApiResponse<Usuario>>(`/usuarios/${id}`);
    return data.data;
  },

  async roles(): Promise<RolResumen[]> {
    const { data } = await api.get<ApiResponse<RolResumen[]>>('/usuarios/roles');
    return data.data;
  },

  async crear(
    peticion: GuardarUsuarioRequest,
  ): Promise<{ alta: AltaRespuesta; mensaje: string }> {
    const { data } = await api.post<ApiResponse<AltaRespuesta>>('/usuarios', peticion);
    return { alta: data.data, mensaje: data.message };
  },

  async actualizar(
    id: number,
    peticion: GuardarUsuarioRequest,
  ): Promise<{ usuario: Usuario; mensaje: string }> {
    const { data } = await api.put<ApiResponse<Usuario>>(`/usuarios/${id}`, peticion);
    return { usuario: data.data, mensaje: data.message };
  },

  async reiniciarPassword(
    id: number,
  ): Promise<{ alta: AltaRespuesta; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<AltaRespuesta>>(
      `/usuarios/${id}/reiniciar-password`,
    );
    return { alta: data.data, mensaje: data.message };
  },

  async cambiarVigencia(
    id: number,
    activa: boolean,
  ): Promise<{ usuario: Usuario; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<Usuario>>(
      `/usuarios/${id}/vigencia`,
      null,
      { params: { activa } },
    );
    return { usuario: data.data, mensaje: data.message };
  },

  async desbloquear(id: number): Promise<{ usuario: Usuario; mensaje: string }> {
    const { data } = await api.patch<ApiResponse<Usuario>>(`/usuarios/${id}/desbloquear`);
    return { usuario: data.data, mensaje: data.message };
  },
};

export default usuarioService;
