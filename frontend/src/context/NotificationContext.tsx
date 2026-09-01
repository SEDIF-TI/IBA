import { Alert, Snackbar } from '@mui/material';
import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

/** Naturaleza del aviso. */
type TipoAviso = 'success' | 'error' | 'warning' | 'info';

interface Aviso {
  mensaje: string;
  tipo: TipoAviso;
  abierto: boolean;
}

interface ContextoNotificacion {
  exito: (mensaje: string) => void;
  error: (mensaje: string) => void;
  advertencia: (mensaje: string) => void;
  info: (mensaje: string) => void;
}

const Contexto = createContext<ContextoNotificacion | undefined>(undefined);

/**
 * Avisos del sistema.
 *
 * <p>Los errores se quedan mas tiempo en pantalla que los aciertos: un
 * "guardado" se entiende de un vistazo, pero un error hay que poder leerlo con
 * calma para saber que corregir.
 */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const [aviso, setAviso] = useState<Aviso>({
    mensaje: '',
    tipo: 'info',
    abierto: false,
  });

  const mostrar = useCallback((mensaje: string, tipo: TipoAviso) => {
    setAviso({ mensaje, tipo, abierto: true });
  }, []);

  const cerrar = useCallback(() => {
    setAviso((anterior) => ({ ...anterior, abierto: false }));
  }, []);

  const valor = useMemo<ContextoNotificacion>(
    () => ({
      exito: (mensaje) => mostrar(mensaje, 'success'),
      error: (mensaje) => mostrar(mensaje, 'error'),
      advertencia: (mensaje) => mostrar(mensaje, 'warning'),
      info: (mensaje) => mostrar(mensaje, 'info'),
    }),
    [mostrar],
  );

  return (
    <Contexto.Provider value={valor}>
      {children}
      <Snackbar
        open={aviso.abierto}
        autoHideDuration={aviso.tipo === 'error' ? 8000 : 4000}
        onClose={cerrar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={cerrar}
          severity={aviso.tipo}
          variant="filled"
          sx={{ width: '100%', maxWidth: 500 }}
          // Los lectores de pantalla deben anunciar el aviso al aparecer.
          role="alert"
        >
          {aviso.mensaje}
        </Alert>
      </Snackbar>
    </Contexto.Provider>
  );
}

/** Acceso a los avisos desde cualquier pantalla. */
export function useNotificacion(): ContextoNotificacion {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useNotificacion debe usarse dentro de NotificationProvider');
  }
  return contexto;
}
