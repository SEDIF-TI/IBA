import api, { borrarToken, guardarToken, renovarSesion } from './api';
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

  /**
   * Cambia la contrasena de quien esta en sesion.
   *
   * <p>El backend cierra las demas sesiones de la cuenta y devuelve una nueva para este
   * dispositivo; su token sustituye al anterior, que deja de servir.
   *
   * @param {CambiarPasswordRequest} peticion - contrasena actual y nueva
   * @returns {Promise<Sesion>} la sesion nueva
   */
  async cambiarPassword(peticion: CambiarPasswordRequest): Promise<Sesion> {
    const { data } = await api.post<ApiResponse<Sesion>>('/auth/cambiar-password', peticion);
    guardarToken(data.data.tokenAcceso);
    return data.data;
  },

  /**
   * Renueva el token a partir de la cookie de refresco.
   * @returns {Promise<Sesion>} la sesion renovada
   */
  refrescar(): Promise<Sesion> {
    return renovarSesion();
  },
};

export default authService;
