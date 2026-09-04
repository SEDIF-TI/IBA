import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import EventIcon from '@mui/icons-material/Event';
import PlaceIcon from '@mui/icons-material/Place';
import ScheduleIcon from '@mui/icons-material/Schedule';
import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { mensajeDeError } from '@/services/api';
import jornadaService from '@/services/jornadaService';
import { guinda } from '@/theme';
import type { JornadaPublica } from '@/types/dominio';

/**
 * Calendario de jornadas para la ciudadania.
 *
 * <p>Sirve para saber cuando toca su municipio y llegar con el tramite
 * adelantado, en lugar de enterarse el mismo dia y formarse sin haber
 * registrado nada.
 */
export function CalendarioJornadas() {
  const [jornadas, setJornadas] = useState<JornadaPublica[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    async function cargar() {
      try {
        setJornadas(await jornadaService.calendarioPublico());
      } catch (fallo) {
        setError(mensajeDeError(fallo));
      } finally {
        setCargando(false);
      }
    }
    void cargar();
  }, []);

  function formatearPeriodo(jornada: JornadaPublica): string {
    const inicio = new Date(jornada.fechaInicio + 'T12:00:00');
    const fin = new Date(jornada.fechaFin + 'T12:00:00');

    const formato: Intl.DateTimeFormatOptions = {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    };

    if (jornada.fechaInicio === jornada.fechaFin) {
      return inicio.toLocaleDateString('es-MX', formato);
    }
    return `Del ${inicio.toLocaleDateString('es-MX', {
      day: 'numeric',
      month: 'long',
    })} al ${fin.toLocaleDateString('es-MX', formato)}`;
  }

  function esHoy(jornada: JornadaPublica): boolean {
    const hoy = new Date().toISOString().slice(0, 10);
    return jornada.fechaInicio <= hoy && hoy <= jornada.fechaFin;
  }

  return (
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Jornadas de empadronamiento
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        El Instituto de Bienestar Animal visita los municipios para empadronar en
        sitio. Adelanta tu registro y el dia del evento solo tendras que
        presentarte.
      </Typography>

      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress aria-label="Cargando el calendario" />
        </Box>
      )}

      {error && (
        <Alert severity="error" role="alert">
          {error}
        </Alert>
      )}

      {!cargando && !error && jornadas.length === 0 && (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 5 }}>
            <EventIcon sx={{ fontSize: 44, color: 'text.disabled' }} aria-hidden />
            <Typography variant="h6" sx={{ mt: 1 }} gutterBottom>
              Por ahora no hay jornadas programadas
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Puedes registrar tu tramite en cualquier momento desde este portal,
              sin esperar a una jornada.
            </Typography>
            <Button component={RouterLink} to="/registro" variant="contained">
              Registrar ahora
            </Button>
          </CardContent>
        </Card>
      )}

      <Stack spacing={2}>
        {jornadas.map((jornada) => (
          <Card
            key={jornada.id}
            sx={{ borderLeft: 5, borderLeftColor: guinda[500] }}
          >
            <CardContent>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <Typography variant="h6">{jornada.nombre}</Typography>
                {esHoy(jornada) && <Chip label="Hoy" color="success" size="small" />}
              </Stack>

              {jornada.descripcion && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                  {jornada.descripcion}
                </Typography>
              )}

              <Stack spacing={0.75}>
                <Stack direction="row" spacing={1} alignItems="flex-start">
                  <EventIcon fontSize="small" color="action" aria-hidden />
                  <Typography variant="body2">{formatearPeriodo(jornada)}</Typography>
                </Stack>

                {jornada.horario && (
                  <Stack direction="row" spacing={1} alignItems="flex-start">
                    <ScheduleIcon fontSize="small" color="action" aria-hidden />
                    <Typography variant="body2">{jornada.horario}</Typography>
                  </Stack>
                )}

                <Stack direction="row" spacing={1} alignItems="flex-start">
                  <PlaceIcon fontSize="small" color="action" aria-hidden />
                  <Box>
                    <Typography variant="body2" fontWeight={500}>
                      {jornada.municipio}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {jornada.domicilioSede}
                    </Typography>
                  </Box>
                </Stack>
              </Stack>

              <Button
                component={RouterLink}
                to={`/registro?jornada=${jornada.id}`}
                variant="contained"
                sx={{ mt: 2 }}
              >
                Adelantar mi registro
              </Button>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {jornadas.length > 0 && (
        <Alert severity="info" sx={{ mt: 3 }}>
          El dia de la jornada lleva tu identificacion oficial original y el folio
          que recibas al registrarte.
        </Alert>
      )}
    </Box>
  );
}

export default CalendarioJornadas;
