/// <reference types="vite/client" />

/**
 * Variables de entorno que usa el frontend.
 *
 * <p>Vite las inyecta al compilar, no en ejecucion: cambiarlas exige volver a
 * construir. Por eso en produccion vale "/api", una ruta relativa que sirve sin
 * importar el dominio donde se despliegue.
 */
interface ImportMetaEnv {
  /** Direccion base del API. En desarrollo la reenvia el proxy de Vite. */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
