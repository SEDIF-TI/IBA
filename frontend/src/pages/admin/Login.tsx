import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  TextField,
  Typography,
} from '@mui/material';
import PetsIcon from '@mui/icons-material/Pets';
import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '@/context/AuthContext';
import { mensajeDeError } from '@/services/api';
import { guinda } from '@/theme';

/**
 * Acceso al panel administrativo.
 *
 * <p>El mensaje de error es el mismo para usuario inexistente y contrasena
 * equivocada: distinguirlos permitiria averiguar que cuentas existen probando
 * nombres.
 */
export function Login() {
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const { iniciarSesion } = useAuth();
  const navegar = useNavigate();
  const ubicacion = useLocation();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);

    try {
      const sesion = await iniciarSesion({ usuario: usuario.trim(), password });

      if (sesion.cambioPasswordRequerido) {
        navegar('/admin/cambiar-password', { replace: true });
        return;
      }

      // Se regresa a donde queria ir antes de que le pidieramos entrar.
      const destino =
        (ubicacion.state as { desde?: { pathname: string } })?.desde?.pathname ?? '/admin';
      navegar(destino, { replace: true });

    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        bgcolor: 'background.default',
      }}
    >
      <Container maxWidth="xs">
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <PetsIcon sx={{ fontSize: 48, color: guinda[500] }} aria-hidden />
          <Typography variant="h5" fontWeight={700} sx={{ mt: 1 }}>
            Padron Estatal de Perros y Gatos
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Instituto de Bienestar Animal
          </Typography>
        </Box>

        <Card>
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Acceso al panel
            </Typography>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }} role="alert">
                {error}
              </Alert>
            )}

            <Box component="form" onSubmit={enviar} noValidate>
              <TextField
                label="Usuario"
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
                fullWidth
                required
                autoFocus
                autoComplete="username"
                margin="normal"
                disabled={enviando}
              />

              <TextField
                label="Contraseña"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                fullWidth
                required
                autoComplete="current-password"
                margin="normal"
                disabled={enviando}
              />

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                sx={{ mt: 3 }}
                disabled={enviando || !usuario.trim() || !password}
              >
                {enviando ? (
                  <CircularProgress size={22} color="inherit" />
                ) : (
                  'Entrar'
                )}
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: 'block', textAlign: 'center', mt: 2 }}
        >
          Este acceso es solo para personal autorizado.
        </Typography>
      </Container>
    </Box>
  );
}

export default Login;
