import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import authService from '@/services/authService';
import { borrarToken } from '@/services/api';
import type { LoginRequest, Sesion } from '@/types/auth';

/**
 * Sesion del personal administrativo.
 *
 * <p>Guarda quien esta trabajando y que puede hacer. La ciudadania no pasa por
 * aqui: el portal publico no tiene sesion.
 */
interface ContextoAuth {
  sesion: Sesion | null;
  cargando: boolean;
  autenticado: boolean;
  iniciarSesion: (credenciales: LoginRequest) => Promise<Sesion>;
  cerrarSesion: () => Promise<void>;
  /** Verdadero si la persona tiene el permiso indicado. */
  tienePermiso: (permiso: string) => boolean;
  /** Verdadero si tiene al menos uno de los permisos. */
  tieneAlgunPermiso: (...permisos: string[]) => boolean;
  tieneRol: (rol: string) => boolean;
  /** Sustituye la sesion por la que devuelve el backend al cambiar la contrasena. */
  reemplazarSesion: (nueva: Sesion) => void;
}

const Contexto = createContext<ContextoAuth | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [cargando, setCargando] = useState(true);

  /**
   * Al abrir el panel se intenta recuperar la sesion con la cookie HttpOnly de refresco.
   *
   * <p>El token de acceso vive solo en memoria, asi que recargar la pagina lo pierde; la cookie
   * permite obtener uno nuevo sin volver a escribir la contrasena, y de paso se releen los
   * permisos. En el portal ciudadano no se intenta: no hay sesion que recuperar y cada intento
   * gastaria el limite de renovaciones de la IP.
   */
  useEffect(() => {
    async function recuperarSesion() {
      if (!window.location.pathname.startsWith('/admin')) {
        setCargando(false);
        return;
      }
      try {
        setSesion(await authService.refrescar());
      } catch {
        // No hay sesion vigente; la persona entra de nuevo.
        borrarToken();
      } finally {
        setCargando(false);
      }
    }
    void recuperarSesion();
  }, []);

  const iniciarSesion = useCallback(async (credenciales: LoginRequest) => {
    const nueva = await authService.iniciarSesion(credenciales);
    setSesion(nueva);
    return nueva;
  }, []);

  /**
   * Cierra la sesion en el servidor y recarga la aplicacion en la pantalla de acceso.
   *
   * <p>La recarga completa borra de la memoria todo lo consultado: quien entre despues en la misma
   * pestaña no ve nada de la sesion anterior.
   */
  const cerrarSesion = useCallback(async () => {
    try {
      await authService.cerrarSesion();
    } finally {
      setSesion(null);
      window.location.replace('/admin/login');
    }
  }, []);

  const tienePermiso = useCallback(
    (permiso: string) => sesion?.permisos.includes(permiso) ?? false,
    [sesion],
  );

  const tieneAlgunPermiso = useCallback(
    (...permisos: string[]) => permisos.some((p) => sesion?.permisos.includes(p)),
    [sesion],
  );

  const tieneRol = useCallback(
    (rol: string) => sesion?.roles.includes(rol) ?? false,
    [sesion],
  );

  const reemplazarSesion = useCallback((nueva: Sesion) => {
    setSesion(nueva);
  }, []);

  const valor = useMemo<ContextoAuth>(
    () => ({
      sesion,
      cargando,
      autenticado: sesion !== null,
      iniciarSesion,
      cerrarSesion,
      tienePermiso,
      tieneAlgunPermiso,
      tieneRol,
      reemplazarSesion,
    }),
    [
      sesion,
      cargando,
      iniciarSesion,
      cerrarSesion,
      tienePermiso,
      tieneAlgunPermiso,
      tieneRol,
      reemplazarSesion,
    ],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

/** Acceso a la sesion desde cualquier pantalla. */
export function useAuth(): ContextoAuth {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return contexto;
}
