import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types/api';

/**
 * Instancia de axios que usa todo el sistema.
 *
 * <p>Aqui se concentran la direccion base, el envio del token y el manejo de
 * respuestas. Ningun componente ni pantalla llama a axios directamente: siempre
 * pasan por un servicio, y los servicios pasan por aqui.
 *
 * <p>La razon es practica: cuando cambie la forma de autenticar, o haya que
 * agregar una cabecera, se toca un archivo y no cuarenta.
 */

/**
 * En desarrollo Vite reenvia /api al backend, asi que basta la ruta relativa.
 * En produccion el proxy inverso hace lo mismo bajo el mismo dominio.
 */
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api';

/** Donde se guarda el token de acceso en el navegador. */
const CLAVE_TOKEN = 'sea_token';

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
  // Necesario para que viaje la cookie del token de refresco.
  withCredentials: true,
});

/** Guarda el token de acceso. */
export function guardarToken(token: string): void {
  localStorage.setItem(CLAVE_TOKEN, token);
}

/** Token de acceso vigente, si lo hay. */
export function obtenerToken(): string | null {
  return localStorage.getItem(CLAVE_TOKEN);
}

/** Borra el token al cerrar sesion. */
export function borrarToken(): void {
  localStorage.removeItem(CLAVE_TOKEN);
}

// Cada peticion sale con el token, si hay sesion abierta.
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = obtenerToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Control de renovacion.
 *
 * <p>Si varias peticiones caducan a la vez, solo la primera pide un token
 * nuevo; las demas esperan a que termine. Sin esto, una pantalla con cinco
 * consultas simultaneas dispararia cinco renovaciones.
 */
let renovando = false;
let esperando: Array<(token: string | null) => void> = [];

function avisarAEsperando(token: string | null): void {
  esperando.forEach((resolver) => resolver(token));
  esperando = [];
}

api.interceptors.response.use(
  (respuesta) => respuesta,

  async (error: AxiosError<ApiResponse<unknown>>) => {
    const peticionOriginal = error.config as InternalAxiosRequestConfig & {
      _reintentada?: boolean;
    };

    // Sesion caducada: se intenta renovar una sola vez.
    if (
      error.response?.status === 401 &&
      peticionOriginal &&
      !peticionOriginal._reintentada &&
      !peticionOriginal.url?.includes('/auth/login') &&
      !peticionOriginal.url?.includes('/auth/refrescar')
    ) {
      if (renovando) {
        // Ya hay una renovacion en curso: esperar su resultado.
        return new Promise((resolver, rechazar) => {
          esperando.push((token) => {
            if (token) {
              peticionOriginal.headers.Authorization = `Bearer ${token}`;
              resolver(api(peticionOriginal));
            } else {
              rechazar(error);
            }
          });
        });
      }

      peticionOriginal._reintentada = true;
      renovando = true;

      try {
        const { data } = await axios.post<ApiResponse<{ tokenAcceso: string }>>(
          `${BASE_URL}/auth/refrescar`,
          {},
          { withCredentials: true },
        );

        const tokenNuevo = data.data.tokenAcceso;
        guardarToken(tokenNuevo);
        avisarAEsperando(tokenNuevo);

        peticionOriginal.headers.Authorization = `Bearer ${tokenNuevo}`;
        return api(peticionOriginal);

      } catch (fallo) {
        // La renovacion tampoco sirvio: hay que volver a entrar.
        avisarAEsperando(null);
        borrarToken();

        // Solo el panel manda a la pantalla de acceso. En el portal ciudadano no
        // hay sesion que renovar: mandar ahi a la persona la deja frente a un
        // acceso de personal que no le corresponde, y pareceria que necesita
        // cuenta para hacer su tramite.
        const enPanel = window.location.pathname.startsWith('/admin');
        const enAcceso = window.location.pathname.startsWith('/admin/login');
        if (enPanel && !enAcceso) {
          // Se conserva a donde queria ir, para regresar despues de entrar.
          const destino = window.location.pathname + window.location.search;
          window.location.href = `/admin/login?destino=${encodeURIComponent(destino)}`;
        }
        return Promise.reject(fallo);

      } finally {
        renovando = false;
      }
    }

    return Promise.reject(error);
  },
);

/**
 * Texto de error listo para mostrar.
 *
 * <p>Se prefiere el mensaje que manda el backend, que ya viene redactado para
 * la persona usuaria. Solo si no hay ninguno se recurre a un texto generico:
 * "Network Error" no le dice nada a quien esta capturando un tramite.
 */
export function mensajeDeError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const respuesta = error.response?.data as ApiResponse<unknown> | undefined;

    if (respuesta?.errors?.length) {
      return respuesta.errors.map((e) => e.mensaje).join('. ');
    }
    if (respuesta?.message) {
      return respuesta.message;
    }
    if (error.code === 'ECONNABORTED') {
      return 'La operacion tardo demasiado. Intenta de nuevo.';
    }
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Revisa tu conexion.';
    }
  }
  return 'Ocurrio un error inesperado. Intenta de nuevo.';
}

export default api;
