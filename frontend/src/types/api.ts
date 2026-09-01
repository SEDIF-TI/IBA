/**
 * Tipos que devuelve el backend.
 *
 * <p>Todas las respuestas comparten la misma envoltura, de modo que el
 * interceptor pueda tratar exito y error en un solo sitio.
 */

/** Envoltura uniforme de toda respuesta del API. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: ErrorCampo[];
  timestamp: string;
}

/** Detalle de un error de validacion, campo por campo. */
export interface ErrorCampo {
  campo: string;
  mensaje: string;
}

/** Pagina de resultados. */
export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** Parametros de paginacion y orden. */
export interface ParametrosPagina {
  page?: number;
  size?: number;
  sort?: string;
}
