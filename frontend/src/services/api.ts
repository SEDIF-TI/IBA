import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types/api';
import type { Sesion } from '@/types/auth';

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


export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
  // Necesario para que viaje la cookie del token de refresco.
  withCredentials: true,
});

/**
 * Token de acceso vigente, SOLO en memoria.
 *
 * <p>No se guarda en localStorage ni en sessionStorage: cualquier script que lograra colarse en la
 * página podría leerlo de ahí y llevarse la sesión. En memoria desaparece al recargar o cerrar la
 * pestaña; entonces la sesión se recupera con la cookie HttpOnly de refresco, que JavaScript no
 * puede leer.
 */
let tokenEnMemoria: string | null = null;

// Versiones anteriores guardaban el token en localStorage; se borra el que haya quedado.
try {
  localStorage.removeItem('sea_token');
} catch {
  // Navegación privada o almacenamiento bloqueado: no hay nada que borrar.
}

/**
 * Guarda el token de acceso en memoria.
 * @param {string} token - token de acceso recibido del backend
 */
export function guardarToken(token: string): void {
  tokenEnMemoria = token;
}

/** @returns {string | null} token de acceso vigente, o null si no hay sesión */
export function obtenerToken(): string | null {
  return tokenEnMemoria;
}

/** Olvida el token de acceso (cierre de sesión o sesión vencida). */
export function borrarToken(): void {
  tokenEnMemoria = null;
}

// Cada peticion sale con el token, si hay sesion abierta.
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = obtenerToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Renovación en curso dentro de esta pestaña; las peticiones que caducan a la vez la comparten. */
let renovacionEnCurso: Promise<Sesion> | null = null;

/**
 * Ejecuta la tarea con un candado compartido entre pestañas (Web Locks API).
 *
 * <p>El backend rota el token de refresco en cada uso y trata como robo un token usado dos veces.
 * Si dos pestañas renovaran exactamente al mismo tiempo con la misma cookie, la segunda cerraría la
 * sesión. Con el candado renuevan una tras otra, y la segunda ya envía la cookie nueva.
 */
function conCandado<T>(tarea: () => Promise<T>): Promise<T> {
  const candados = typeof navigator !== 'undefined' ? navigator.locks : undefined;
  return candados
    ? (candados.request('sea-renovacion-sesion', tarea) as Promise<T>)
    : tarea();
}

/**
 * Pide un token de acceso nuevo con la cookie de refresco y lo guarda en memoria.
 *
 * @returns {Promise<Sesion>} la sesión renovada, con permisos releídos del backend
 * @throws error de axios si la sesión ya no es válida (401)
 */
export function renovarSesion(): Promise<Sesion> {
  if (!renovacionEnCurso) {
    renovacionEnCurso = conCandado(async () => {
      const { data } = await axios.post<ApiResponse<Sesion>>(
        `${BASE_URL}/auth/refrescar`,
        {},
        { withCredentials: true },
      );
      guardarToken(data.data.tokenAcceso);
      return data.data;
    }).finally(() => {
      renovacionEnCurso = null;
    });
  }
  return renovacionEnCurso;
}

api.interceptors.response.use(
  (respuesta) => respuesta,

  async (error: AxiosError<ApiResponse<unknown>>) => {
    const peticionOriginal = error.config as InternalAxiosRequestConfig & {
      _reintentada?: boolean;
    };
    const url = peticionOriginal?.url ?? '';

    // Sesion caducada: se intenta renovar una sola vez.
    if (
      error.response?.status === 401 &&
      peticionOriginal &&
      !peticionOriginal._reintentada &&
      !url.includes('/auth/login') &&
      !url.includes('/auth/refrescar') &&
      !url.includes('/auth/logout')
    ) {
      peticionOriginal._reintentada = true;
      try {
        const sesion = await renovarSesion();
        peticionOriginal.headers.Authorization = `Bearer ${sesion.tokenAcceso}`;
        return api(peticionOriginal);
      } catch (fallo) {
        // La renovacion tampoco sirvio: hay que volver a entrar.
        borrarToken();

        // Solo el panel manda a la pantalla de acceso. En el portal ciudadano no
        // hay sesion que renovar: mandar ahi a la persona la deja frente a un
        // acceso de personal que no le corresponde.
        const enPanel = window.location.pathname.startsWith('/admin');
        const enAcceso = window.location.pathname.startsWith('/admin/login');
        if (enPanel && !enAcceso) {
          // Recarga completa: borra de la memoria todo lo consultado por la sesion anterior.
          const destino = window.location.pathname + window.location.search;
          window.location.href = `/admin/login?destino=${encodeURIComponent(destino)}`;
        }
        return Promise.reject(fallo);
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
