/** Tipos de la sesion administrativa. */

/** Credenciales de acceso. */
export interface LoginRequest {
  usuario: string;
  password: string;
}

/**
 * Datos de la sesion.
 *
 * <p>El token de refresco no viene aqui: viaja en una cookie que el navegador
 * maneja solo y JavaScript no puede leer.
 */
export interface Sesion {
  tokenAcceso: string;
  tipo: string;
  expiraEn: number;
  usuario: string;
  nombreCompleto: string;
  roles: string[];
  permisos: string[];
  cambioPasswordRequerido: boolean;
}

/** Cambio de contrasena de quien esta en sesion. */
export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}
