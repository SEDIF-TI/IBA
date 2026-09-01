import api, { borrarToken, guardarToken } from './api';
import type { ApiResponse } from '@/types/api';
import type { CambiarPasswordRequest, LoginRequest, Sesion } from '@/types/auth';

/**
 * Acceso al panel administrativo.
 *
 * <p>Solo el personal se autentica: la ciudadania usa el portal sin cuenta.
 */
export const authService = {
  /** Inicia sesion y guarda el token de acceso. */
  async iniciarSesion(credenciales: LoginRequest): Promise<Sesion> {
    const { data } = await api.post<ApiResponse<Sesion>>('/auth/login', credenciales);
    guardarToken(data.data.tokenAcceso);
    return data.data;
  },

  /**
   * Cierra la sesion.
   *
   * <p>El token local se borra pase lo que pase: si la llamada falla, la
   * persona igual espera haber salido.
   */
  async cerrarSesion(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      borrarToken();
    }
  },

  /** Cambia la contrasena de quien esta en sesion. */
  async cambiarPassword(peticion: CambiarPasswordRequest): Promise<void> {
    await api.post('/auth/cambiar-password', peticion);
  },

  /** Renueva el token a partir de la cookie de refresco. */
  async refrescar(): Promise<Sesion> {
    const { data } = await api.post<ApiResponse<Sesion>>('/auth/refrescar');
    guardarToken(data.data.tokenAcceso);
    return data.data;
  },
};

export default authService;
