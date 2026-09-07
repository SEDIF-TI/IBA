import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import PersonIcon from '@mui/icons-material/Person';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { padronService, tutorService } from '@/services/padronService';
import { guinda } from '@/theme';
import type { Animal, Titularidad, Tutor } from '@/types/dominio';

/**
 * Ficha de un animal empadronado.
 *
 * <p>Muestra sus datos, quien responde por el y el historial completo de
 * tutores: un animal se transfiere o se adopta, y saber a quien correspondia y
 * hasta cuando es parte del padron.
 */
export function FichaAnimal() {
  const { id } = useParams<{ id: string }>();
  const animalId = Number(id);

  const [animal, setAnimal] = useState<Animal | null>(null);
  const [historial, setHistorial] = useState<Titularidad[]>([]);
  const [cargando, setCargando] = useState(true);

  const [transferirAbierto, setTransferirAbierto] = useState(false);
  const [busquedaTutor, setBusquedaTutor] = useState('');
  const [candidatos, setCandidatos] = useState<Tutor[]>([]);
  const [nuevoTutor, setNuevoTutor] = useState<Tutor | null>(null);
  const [motivo, setMotivo] = useState('');
  const [procesando, setProcesando] = useState(false);

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [ficha, tutores] = await Promise.all([
        padronService.obtener(animalId),
        padronService.historialTitularidad(animalId),
      ]);
      setAnimal(ficha);
      setHistorial(tutores);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animalId]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  // Busca al tutor que recibira el animal, mientras se escribe.
  useEffect(() => {
    if (!transferirAbierto || busquedaTutor.trim().length < 3) {
      setCandidatos([]);
      return;
    }
    const temporizador = setTimeout(async () => {
      try {
        const { resultados } = await tutorService.buscar(busquedaTutor, { size: 8 });
        setCandidatos(resultados.content);
      } catch {
        setCandidatos([]);
      }
    }, 400);
    return () => clearTimeout(temporizador);
  }, [busquedaTutor, transferirAbierto]);

  async function transferir() {
    if (!nuevoTutor) return;

    setProcesando(true);
    try {
      const { mensaje } = await padronService.transferir(animalId, nuevoTutor.id, motivo);
      notificacion.exito(mensaje);
      cerrarTransferencia();
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setProcesando(false);
    }
  }

  function cerrarTransferencia() {
    setTransferirAbierto(false);
    setBusquedaTutor('');
    setCandidatos([]);
    setNuevoTutor(null);
    setMotivo('');
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando la ficha" />
      </Box>
    );
  }

  if (!animal) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">No se encontro el animal</Typography>
        <Button onClick={() => navegar('/admin/padron')} sx={{ mt: 2 }}>
          Volver al padron
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navegar('/admin/padron')} aria-label="Volver">
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" fontWeight={600}>
            {animal.nombre}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {[animal.especie, animal.raza].filter(Boolean).join(' - ')}
          </Typography>
        </Box>
        {animal.finado && <Chip label="Finado" variant="outlined" />}
      </Stack>

      {animal.finado && animal.fechaDefuncion && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Se registro su defuncion el{' '}
          {new Date(animal.fechaDefuncion + 'T12:00:00').toLocaleDateString('es-MX')}. El
          registro permanece en el padron con su historial completo.
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Datos del animal
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Dato etiqueta="Identificador" valor={animal.claveAnimal} monoespaciado />
              <Dato etiqueta="Especie" valor={animal.especie} />
              <Dato etiqueta="Raza" valor={animal.raza} />
              <Dato etiqueta="Sexo" valor={animal.sexo} />
              <Dato etiqueta="Color" valor={animal.color} />
              <Dato etiqueta="Tamano" valor={animal.tamano} />
              <Dato etiqueta="Edad" valor={animal.edadDescriptiva} />
              <Dato etiqueta="Vacunado" valor={animal.vacunado ? 'Si' : 'No'} />
              <Dato etiqueta="Esterilizado" valor={animal.esterilizado ? 'Si' : 'No'} />
              {animal.identificador && (
                <Dato
                  etiqueta={animal.metodoIdentificacion ?? 'Identificacion'}
                  valor={animal.identificador}
                  monoespaciado
                />
              )}
              <Dato etiqueta="Senas" valor={animal.caracteristicas} />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Stack
                direction="row"
                justifyContent="space-between"
                alignItems="center"
                sx={{ mb: 1 }}
              >
                <Typography variant="subtitle1" fontWeight={600}>
                  Tutor responsable
                </Typography>
                {tienePermiso('titularidad:transferir') && !animal.finado && (
                  <Button
                    size="small"
                    startIcon={<SwapHorizIcon />}
                    onClick={() => setTransferirAbierto(true)}
                  >
                    Transferir
                  </Button>
                )}
              </Stack>
              <Divider sx={{ mb: 2 }} />

              {animal.tutorNombre ? (
                <Box
                  onClick={() => navegar(`/admin/tutores/${animal.tutorId}`)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    p: 1.5,
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: 1,
                    cursor: 'pointer',
                    '&:hover': { bgcolor: guinda[50] },
                  }}
                >
                  <PersonIcon color="action" />
                  <Typography variant="body1">{animal.tutorNombre}</Typography>
                </Box>
              ) : (
                <Alert severity="warning">
                  Este animal no tiene tutor responsable vigente. Asignale uno con
                  el boton Transferir.
                </Alert>
              )}

              <Typography variant="overline" color="text.secondary" sx={{ mt: 3, display: 'block' }}>
                Historial de tutores
              </Typography>

              <Stack spacing={1} sx={{ mt: 1 }}>
                {historial.map((tramo) => (
                  <Box
                    key={tramo.id}
                    sx={{
                      p: 1.5,
                      borderLeft: 3,
                      borderLeftColor: tramo.vigente ? 'success.main' : 'divider',
                      bgcolor: tramo.vigente ? 'transparent' : 'action.hover',
                      borderRadius: 0.5,
                    }}
                  >
                    <Typography variant="body2" fontWeight={500}>
                      {tramo.tutorNombre}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Desde el{' '}
                      {new Date(tramo.fechaInicio + 'T12:00:00').toLocaleDateString('es-MX')}
                      {tramo.fechaFin
                        ? ` hasta el ${new Date(
                            tramo.fechaFin + 'T12:00:00',
                          ).toLocaleDateString('es-MX')}`
                        : ' (vigente)'}
                    </Typography>
                    {tramo.motivoBaja && (
                      <Typography variant="caption" color="text.secondary">
                        {tramo.motivoBaja}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Dialog
        open={transferirAbierto}
        onClose={cerrarTransferencia}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Transferir a otro tutor</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            El tutor actual queda en el historial del animal con la fecha y el
            motivo. No se borra nada.
          </Alert>

          {nuevoTutor ? (
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                p: 2,
                border: 2,
                borderColor: 'primary.main',
                borderRadius: 1,
                mb: 2,
              }}
            >
              <PersonIcon color="primary" />
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body1" fontWeight={500}>
                  {nuevoTutor.nombreCompleto}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {nuevoTutor.curpEnmascarada ?? 'Sin CURP'} -{' '}
                  {nuevoTutor.animalesACargo} animal(es) a su cargo
                </Typography>
              </Box>
              <Button size="small" onClick={() => setNuevoTutor(null)}>
                Cambiar
              </Button>
            </Box>
          ) : (
            <>
              <TextField
                label="Busca al nuevo tutor"
                value={busquedaTutor}
                onChange={(e) => setBusquedaTutor(e.target.value)}
                fullWidth
                autoFocus
                margin="normal"
                helperText="Escribe al menos tres letras del apellido, o su CURP completa"
              />

              <Stack spacing={0.5} sx={{ mb: 2 }}>
                {candidatos.map((candidato) => (
                  <Box
                    key={candidato.id}
                    onClick={() => setNuevoTutor(candidato)}
                    sx={{
                      p: 1.5,
                      border: 1,
                      borderColor: 'divider',
                      borderRadius: 1,
                      cursor: 'pointer',
                      '&:hover': { bgcolor: guinda[50] },
                    }}
                  >
                    <Typography variant="body2">{candidato.nombreCompleto}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {candidato.curpEnmascarada ?? 'Sin CURP'}
                      {candidato.municipio ? ` - ${candidato.municipio}` : ''}
                    </Typography>
                  </Box>
                ))}
                {busquedaTutor.trim().length >= 3 && candidatos.length === 0 && (
                  <Typography variant="caption" color="text.secondary">
                    Ningun tutor coincide. Verifica el apellido o registra primero a
                    la persona con un trámite.
                  </Typography>
                )}
              </Stack>
            </>
          )}

          <TextField
            label="Motivo de la transferencia"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            fullWidth
            required
            multiline
            rows={2}
            helperText="Adopción, cambio de responsable, fallecimiento del tutor..."
            inputProps={{ maxLength: 300 }}
          />
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={cerrarTransferencia} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            onClick={transferir}
            disabled={procesando || !nuevoTutor || !motivo.trim()}
          >
            {procesando ? 'Transfiriendo...' : 'Transferir'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

/** Etiqueta con su valor. */
function Dato({
  etiqueta,
  valor,
  monoespaciado = false,
}: {
  etiqueta: string;
  valor?: string | null;
  monoespaciado?: boolean;
}) {
  if (!valor) {
    return null;
  }
  return (
    <Box sx={{ display: 'flex', gap: 1, py: 0.5 }}>
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 110, flexShrink: 0 }}>
        {etiqueta}
      </Typography>
      <Typography variant="body2" fontFamily={monoespaciado ? 'monospace' : undefined}>
        {valor}
      </Typography>
    </Box>
  );
}

export default FichaAnimal;
