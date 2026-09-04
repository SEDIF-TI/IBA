import { Box, CircularProgress } from '@mui/material';
import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';

import { useAuth } from '@/context/AuthContext';

interface Props {
  children: ReactNode;
  /** Permiso necesario para entrar; sin el, basta con estar autenticado. */
  permiso?: string;
}

/**
 * Guarda de las rutas del panel.
 *
 * <p>Es una comodidad para la persona usuaria, no una medida de seguridad: el
 * backend vuelve a comprobar el permiso en cada peticion. Ocultar un boton no
 * protege nada; lo que protege es que el servidor diga que no.
 */
export function ProtectedRoute({ children, permiso }: Props) {
  const { autenticado, cargando, sesion, tienePermiso } = useAuth();
  const ubicacion = useLocation();

  // Mientras se recupera la sesion no se decide nada: redirigir aqui sacaria
  // a quien si tiene sesion valida cada vez que recarga la pagina.
  if (cargando) {
    return (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress aria-label="Verificando sesión" />
      </Box>
    );
  }

  if (!autenticado) {
    // Se recuerda a donde queria ir, para llevarla ahi despues de entrar.
    return <Navigate to="/admin/login" state={{ desde: ubicacion }} replace />;
  }

  // Con contrasena por cambiar no se puede operar: es el paso que cierra el
  // hueco de tener una contrasena inicial escrita en un archivo de configuracion.
  if (sesion?.cambioPasswordRequerido && ubicacion.pathname !== '/admin/cambiar-password') {
    return <Navigate to="/admin/cambiar-password" replace />;
  }

  if (permiso && !tienePermiso(permiso)) {
    return <Navigate to="/admin/sin-permiso" replace />;
  }

  return <>{children}</>;
}

export default ProtectedRoute;
