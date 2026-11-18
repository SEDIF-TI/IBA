import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import authService from '@/services/authService';
import { mensajeDeError } from '@/services/api';

/** Requisitos que debe cumplir la contrasena nueva. */
const REGLAS = [
  { texto: 'Al menos 12 caracteres', cumple: (v: string) => v.length >= 12 },
  { texto: 'Una mayuscula', cumple: (v: string) => /[A-Z]/.test(v) },
  { texto: 'Una minuscula', cumple: (v: string) => /[a-z]/.test(v) },
  { texto: 'Un numero', cumple: (v: string) => /\d/.test(v) },
];

/**
 * Cambio de contrasena.
 *
 * <p>Es obligatorio en el primer acceso: la contrasena inicial vive en un
 * archivo de configuracion, asi que no debe seguir siendo valida.
 */
export function CambiarPassword() {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const { sesion, reemplazarSesion } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const reglasCumplidas = REGLAS.every((regla) => regla.cumple(nueva));
  const coinciden = nueva.length > 0 && nueva === confirmacion;
  const puedeEnviar = actual.length > 0 && reglasCumplidas && coinciden && !enviando;

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);

    try {
      const nuevaSesion = await authService.cambiarPassword({
        passwordActual: actual,
        passwordNueva: nueva,
      });
      reemplazarSesion(nuevaSesion);
      notificacion.exito(
        'Tu contraseña quedó actualizada. Se cerró la sesión en tus otros dispositivos.',
      );
      navegar('/admin', { replace: true });
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', py: 4 }}>
      <Container maxWidth="sm">
        <Card>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Cambiar contrasena
            </Typography>

            {sesion?.cambioPasswordRequerido && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Debes definir una contrasena propia antes de continuar.
              </Alert>
            )}

            {error && (
              <Alert severity="error" sx={{ mb: 2 }} role="alert">
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={enviar} noValidate>
              <TextField
                label="Contraseña actual"
                type="password"
                value={actual}
                onChange={(e) => setActual(e.target.value)}
                fullWidth
                required
                autoFocus
                autoComplete="current-password"
                margin="normal"
                disabled={enviando}
              />

              <TextField
                label="Contraseña nueva"
                type="password"
                value={nueva}
                onChange={(e) => setNueva(e.target.value)}
                fullWidth
                required
                autoComplete="new-password"
                margin="normal"
                disabled={enviando}
              />

              <Box component="ul" sx={{ pl: 2.5, my: 1 }}>
                {REGLAS.map((regla) => (
                  <Typography
                    key={regla.texto}
                    component="li"
                    variant="caption"
                    sx={{
                      color: regla.cumple(nueva) ? 'success.main' : 'text.secondary',
                    }}
                  >
                    {regla.texto}
                  </Typography>
                ))}
              </Box>

              <TextField
                label="Confirmar contraseña nueva"
                type="password"
                value={confirmacion}
                onChange={(e) => setConfirmacion(e.target.value)}
                fullWidth
                required
                autoComplete="new-password"
                margin="normal"
                disabled={enviando}
                error={confirmacion.length > 0 && !coinciden}
                helperText={
                  confirmacion.length > 0 && !coinciden
                    ? 'Las contrasenas no coinciden'
                    : ' '
                }
              />

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                sx={{ mt: 2 }}
                disabled={!puedeEnviar}
              >
                Guardar
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}

export default CambiarPassword;
