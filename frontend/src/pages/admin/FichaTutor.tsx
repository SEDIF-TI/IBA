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
import BadgeIcon from '@mui/icons-material/Badge';
import PetsIcon from '@mui/icons-material/Pets';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import EstatusChip from '@/components/EstatusChip';
import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { tutorService } from '@/services/padronService';
import { guinda } from '@/theme';
import type { Animal, Solicitud, TutorDetalle } from '@/types/dominio';

/**
 * Ficha de un tutor responsable.
 *
 * <p>Reune lo que el personal necesita cuando alguien llega preguntando: sus
 * datos, cuantos animales tiene a su cargo y en que van sus tramites.
 *
 * <p>La CURP se muestra completa porque hay que cotejarla contra el documento.
 * Por eso abrir esta ficha queda registrado en bitacora.
 */
export function FichaTutor() {
  const { id } = useParams<{ id: string }>();
  const tutorId = Number(id);

  const [tutor, setTutor] = useState<TutorDetalle | null>(null);
  const [animales, setAnimales] = useState<Animal[]>([]);
  const [tramites, setTramites] = useState<Solicitud[]>([]);
  const [cargando, setCargando] = useState(true);

  const [curpAbierta, setCurpAbierta] = useState(false);
  const [curp, setCurp] = useState('');
  const [guardando, setGuardando] = useState(false);

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [ficha, susAnimales, susTramites] = await Promise.all([
        tutorService.obtener(tutorId),
        tutorService.animales(tutorId),
        tutorService.tramites(tutorId, { size: 20 }),
      ]);
      setTutor(ficha);
      setAnimales(susAnimales);
      setTramites(susTramites.content);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tutorId]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  async function completarCurp() {
    setGuardando(true);
    try {
      const { mensaje } = await tutorService.completarCurp(tutorId, curp.trim().toUpperCase());
      notificacion.exito(mensaje);
      setCurpAbierta(false);
      setCurp('');
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando la ficha" />
      </Box>
    );
  }

  if (!tutor) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">No se encontro el tutor</Typography>
        <Button onClick={() => navegar('/admin/tutores')} sx={{ mt: 2 }}>
          Volver a tutores
        </Button>
      </Box>
    );
  }

  const domicilio = [
    tutor.calle && `${tutor.calle} ${tutor.numeroExterior ?? ''}`.trim(),
    tutor.numeroInterior && `int. ${tutor.numeroInterior}`,
    tutor.colonia && `Col. ${tutor.colonia}`,
    tutor.municipio,
    tutor.codigoPostal && `C.P. ${tutor.codigoPostal}`,
  ]
    .filter(Boolean)
    .join(', ');

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navegar('/admin/tutores')} aria-label="Volver">
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" fontWeight={600}>
            {tutor.nombreCompleto}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Tutor responsable de {tutor.animalesACargo} animal(es)
          </Typography>
        </Box>
      </Stack>

      {!tutor.curp && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          action={
            tienePermiso('persona:editar') && (
              <Button size="small" onClick={() => setCurpAbierta(true)}>
                Capturar CURP
              </Button>
            )
          }
        >
          Este tutor se registro sin CURP. Cuando presente su identificación,
          captura la CURP para completar su identidad.
        </Alert>
      )}

      {tutor.curp && !tutor.identidadValidada && (
        <Alert severity="info" sx={{ mb: 2 }}>
          La CURP esta registrada pero nadie la ha cotejado contra el documento
          oficial.
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Datos del tutor
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Dato etiqueta="CURP" valor={tutor.curp} monoespaciado />
              <Dato
                etiqueta="Nacimiento"
                valor={
                  tutor.fechaNacimiento
                    ? new Date(tutor.fechaNacimiento + 'T12:00:00').toLocaleDateString('es-MX')
                    : undefined
                }
              />
              <Dato etiqueta="Entidad" valor={tutor.entidadNacimientoNombre} />
              <Dato etiqueta="Correo" valor={tutor.correo} />
              <Dato etiqueta="Telefono" valor={tutor.telefono} />
              <Dato etiqueta="WhatsApp" valor={tutor.telefonoWhatsapp} />
              <Dato etiqueta="Domicilio" valor={domicilio || undefined} />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Animales a su cargo
              </Typography>
              <Divider sx={{ mb: 2 }} />

              {animales.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No tiene animales a su cargo actualmente
                </Typography>
              ) : (
                <Stack spacing={1}>
                  {animales.map((animal) => (
                    <Box
                      key={animal.id}
                      onClick={() => navegar(`/admin/padron/${animal.id}`)}
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
                      <PetsIcon color="action" />
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="body1" fontWeight={500}>
                          {animal.nombre}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {[animal.especie, animal.raza, animal.sexo]
                            .filter(Boolean)
                            .join(' - ')}
                        </Typography>
                      </Box>
                      {animal.finado && <Chip label="Finado" size="small" variant="outlined" />}
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Sus trámites
          </Typography>
          <Divider sx={{ mb: 2 }} />

          {tramites.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No tiene trámites registrados
            </Typography>
          ) : (
            <Stack spacing={1}>
              {tramites.map((tramite) => (
                <Box
                  key={tramite.id}
                  onClick={() => navegar(`/admin/solicitudes/${tramite.id}`)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    p: 1.5,
                    border: 1,
                    borderColor: 'divider',
                    borderRadius: 1,
                    cursor: 'pointer',
                    '&:hover': { bgcolor: guinda[50] },
                  }}
                >
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="body2" fontFamily="monospace">
                      {tramite.folio}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {tramite.animalNombre} -{' '}
                      {new Date(tramite.fechaRegistro).toLocaleDateString('es-MX')}
                    </Typography>
                  </Box>
                  <EstatusChip
                    clave={tramite.estatusClave}
                    nombre={tramite.estatusNombre}
                    color={tramite.estatusColor}
                  />
                </Box>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>

      <Dialog open={curpAbierta} onClose={() => setCurpAbierta(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Capturar CURP</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Captura la CURP tal como aparece en el documento que presento la
            persona. Al guardarla, su identidad queda validada.
          </Alert>
          <TextField
            label="CURP"
            value={curp}
            onChange={(e) => setCurp(e.target.value.toUpperCase())}
            fullWidth
            required
            autoFocus
            inputProps={{ maxLength: 18, style: { fontFamily: 'monospace' } }}
            helperText={`${curp.length} de 18 caracteres`}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCurpAbierta(false)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            startIcon={<BadgeIcon />}
            onClick={completarCurp}
            disabled={guardando || curp.length !== 18}
          >
            {guardando ? 'Guardando...' : 'Guardar'}
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
  return (
    <Box sx={{ display: 'flex', gap: 1, py: 0.5 }}>
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 100, flexShrink: 0 }}>
        {etiqueta}
      </Typography>
      <Typography variant="body2" fontFamily={monoespaciado ? 'monospace' : undefined}>
        {valor ?? '-'}
      </Typography>
    </Box>
  );
}

export default FichaTutor;
