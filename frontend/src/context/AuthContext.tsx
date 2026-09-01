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
import { borrarToken, obtenerToken } from '@/services/api';
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
  /** Se invoca tras cambiar la contrasena, para levantar la marca. */
  marcarPasswordCambiada: () => void;
}

const Contexto = createContext<ContextoAuth | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [cargando, setCargando] = useState(true);

  /**
   * Al abrir la aplicacion se intenta recuperar la sesion.
   *
   * <p>Si hay token guardado se pide uno nuevo con la cookie de refresco. Asi
   * recargar la pagina no obliga a volver a entrar, y de paso los permisos se
   * releen: si a alguien se le retiro uno, se entera aqui.
   */
  useEffect(() => {
    async function recuperarSesion() {
      if (!obtenerToken()) {
        setCargando(false);
        return;
      }
      try {
        setSesion(await authService.refrescar());
      } catch {
        // La sesion ya no sirve; se limpia y la persona vuelve a entrar.
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

  const cerrarSesion = useCallback(async () => {
    await authService.cerrarSesion();
    setSesion(null);
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

  const marcarPasswordCambiada = useCallback(() => {
    setSesion((anterior) =>
      anterior ? { ...anterior, cambioPasswordRequerido: false } : anterior,
    );
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
      marcarPasswordCambiada,
    }),
    [
      sesion,
      cargando,
      iniciarSesion,
      cerrarSesion,
      tienePermiso,
      tieneAlgunPermiso,
      tieneRol,
      marcarPasswordCambiada,
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
