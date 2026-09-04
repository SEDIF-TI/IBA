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
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import PublicIcon from '@mui/icons-material/Public';
import PublicOffIcon from '@mui/icons-material/PublicOff';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { catalogos } from '@/services/catalogoService';
import jornadaService from '@/services/jornadaService';
import { guinda } from '@/theme';
import type { Catalogo, Jornada, JornadaRequest } from '@/types/dominio';

const FORMULARIO_VACIO: JornadaRequest = {
  nombre: '',
  descripcion: '',
  municipioId: 0,
  domicilioSede: '',
  fechaInicio: '',
  fechaFin: '',
  horario: '',
};

/**
 * Administracion de jornadas municipales.
 *
 * <p>Las que se celebran hoy aparecen primero y con acceso directo a la vista
 * de operacion: es lo que el personal necesita al llegar al evento.
 */
export function AdminJornadas() {
  const [jornadas, setJornadas] = useState<Jornada[]>([]);
  const [municipios, setMunicipios] = useState<Catalogo[]>([]);
  const [cargando, setCargando] = useState(true);

  const [formularioAbierto, setFormularioAbierto] = useState(false);
  const [editando, setEditando] = useState<Jornada | null>(null);
  const [formulario, setFormulario] = useState<JornadaRequest>(FORMULARIO_VACIO);
  const [guardando, setGuardando] = useState(false);

  const [porDespublicar, setPorDespublicar] = useState<Jornada | null>(null);
  const [motivoRetiro, setMotivoRetiro] = useState('');

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      setJornadas(await jornadaService.listar());
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    void cargar();
    catalogos.municipios.opciones().then(setMunicipios).catch(() => setMunicipios([]));
  }, [cargar]);

  function abrirAlta() {
    setEditando(null);
    setFormulario(FORMULARIO_VACIO);
    setFormularioAbierto(true);
  }

  function abrirEdicion(jornada: Jornada) {
    setEditando(jornada);
    setFormulario({
      nombre: jornada.nombre,
      descripcion: jornada.descripcion ?? '',
      municipioId: jornada.municipioId ?? 0,
      domicilioSede: jornada.domicilioSede,
      fechaInicio: jornada.fechaInicio,
      fechaFin: jornada.fechaFin,
      horario: jornada.horario ?? '',
      cupo: jornada.cupo,
    });
    setFormularioAbierto(true);
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault();
    setGuardando(true);
    try {
      if (editando) {
        await jornadaService.actualizar(editando.id, formulario);
        notificacion.exito(`Se guardaron los cambios de "${formulario.nombre}"`);
      } else {
        await jornadaService.crear(formulario);
        notificacion.exito(
          `La jornada "${formulario.nombre}" quedo registrada. Aun no aparece en ` +
            'el portal: publicala cuando la sede y las fechas esten confirmadas.',
        );
      }
      setFormularioAbierto(false);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  async function publicar(jornada: Jornada) {
    try {
      await jornadaService.publicar(jornada.id);
      notificacion.exito(
        `"${jornada.nombre}" ya aparece en el portal. La ciudadania puede verla ` +
          'y adelantar su registro.',
      );
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  async function despublicar() {
    if (!porDespublicar) return;
    try {
      await jornadaService.despublicar(porDespublicar.id, motivoRetiro);
      notificacion.exito(
        `"${porDespublicar.nombre}" dejo de aparecer en el portal. Los registros ` +
          'que la gente ya adelanto se conservan.',
      );
      setPorDespublicar(null);
      setMotivoRetiro('');
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  const enCurso = jornadas.filter((j) => j.enCurso);
  const otras = jornadas.filter((j) => !j.enCurso);

  return (
    <Box>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
        spacing={2}
        sx={{ mb: 3 }}
      >
        <Box>
          <Typography variant="h5" fontWeight={600}>
            Jornadas municipales
          </Typography>
          <Typography variant="body2" color="text.secondary">
            El personal viaja al municipio; la gente adelanta su registro y el dia
            del evento solo se presenta.
          </Typography>
        </Box>

        {tienePermiso('jornada:crear') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
            Nueva jornada
          </Button>
        )}
      </Stack>

      {cargando ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress aria-label="Cargando jornadas" />
        </Box>
      ) : jornadas.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Typography color="text.secondary">
              Todavia no hay jornadas registradas
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <>
          {enCurso.length > 0 && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="overline" color="text.secondary">
                Hoy
              </Typography>
              <Stack spacing={2} sx={{ mt: 1 }}>
                {enCurso.map((jornada) => (
                  <TarjetaJornada
                    key={jornada.id}
                    jornada={jornada}
                    destacada
                    tienePermiso={tienePermiso}
                    onOperar={() => navegar(`/admin/jornadas/${jornada.id}/operar`)}
                    onEditar={() => abrirEdicion(jornada)}
                    onPublicar={() => publicar(jornada)}
                    onDespublicar={() => setPorDespublicar(jornada)}
                  />
                ))}
              </Stack>
            </Box>
          )}

          {otras.length > 0 && (
            <Box>
              {enCurso.length > 0 && (
                <Typography variant="overline" color="text.secondary">
                  Otras jornadas
                </Typography>
              )}
              <Stack spacing={2} sx={{ mt: 1 }}>
                {otras.map((jornada) => (
                  <TarjetaJornada
                    key={jornada.id}
                    jornada={jornada}
                    tienePermiso={tienePermiso}
                    onOperar={() => navegar(`/admin/jornadas/${jornada.id}/operar`)}
                    onEditar={() => abrirEdicion(jornada)}
                    onPublicar={() => publicar(jornada)}
                    onDespublicar={() => setPorDespublicar(jornada)}
                  />
                ))}
              </Stack>
            </Box>
          )}
        </>
      )}

      <Dialog
        open={formularioAbierto}
        onClose={() => setFormularioAbierto(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={guardar}>
          <DialogTitle>{editando ? 'Editar jornada' : 'Nueva jornada'}</DialogTitle>

          <DialogContent>
            <TextField
              label="Nombre de la jornada"
              value={formulario.nombre}
              onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
              fullWidth
              required
              margin="normal"
              helperText="Es como se anuncia en el portal"
              inputProps={{ maxLength: 200 }}
            />

            <FormControl fullWidth margin="normal" required>
              <InputLabel id="etiqueta-municipio-jornada">Municipio</InputLabel>
              <Select
                labelId="etiqueta-municipio-jornada"
                label="Municipio"
                value={formulario.municipioId || ''}
                onChange={(e) =>
                  setFormulario({ ...formulario, municipioId: Number(e.target.value) })
                }
              >
                {municipios.map((municipio) => (
                  <MenuItem key={municipio.id} value={municipio.id}>
                    {municipio.nombre}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Direccion de la sede"
              value={formulario.domicilioSede}
              onChange={(e) =>
                setFormulario({ ...formulario, domicilioSede: e.target.value })
              }
              fullWidth
              required
              margin="normal"
              helperText="Es a donde va a llegar la gente: escribela completa"
              inputProps={{ maxLength: 400 }}
            />

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <TextField
                  label="Del dia"
                  type="date"
                  value={formulario.fechaInicio}
                  onChange={(e) =>
                    setFormulario({ ...formulario, fechaInicio: e.target.value })
                  }
                  fullWidth
                  required
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  label="Al dia"
                  type="date"
                  value={formulario.fechaFin}
                  onChange={(e) => setFormulario({ ...formulario, fechaFin: e.target.value })}
                  fullWidth
                  required
                  margin="normal"
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>
            </Grid>

            <Grid container spacing={2}>
              <Grid item xs={7}>
                <TextField
                  label="Horario"
                  value={formulario.horario}
                  onChange={(e) => setFormulario({ ...formulario, horario: e.target.value })}
                  fullWidth
                  margin="normal"
                  placeholder="9:00 a 15:00 horas"
                  inputProps={{ maxLength: 120 }}
                />
              </Grid>
              <Grid item xs={5}>
                <TextField
                  label="Cupo"
                  type="number"
                  value={formulario.cupo ?? ''}
                  onChange={(e) =>
                    setFormulario({
                      ...formulario,
                      cupo: e.target.value ? parseInt(e.target.value, 10) : undefined,
                    })
                  }
                  fullWidth
                  margin="normal"
                  helperText="Vacio si no hay limite"
                />
              </Grid>
            </Grid>

            <TextField
              label="Descripcion"
              value={formulario.descripcion}
              onChange={(e) => setFormulario({ ...formulario, descripcion: e.target.value })}
              fullWidth
              multiline
              rows={2}
              margin="normal"
              helperText="Se muestra en el portal"
              inputProps={{ maxLength: 1000 }}
            />
          </DialogContent>

          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setFormularioAbierto(false)} color="inherit">
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={
                guardando ||
                !formulario.nombre ||
                !formulario.municipioId ||
                !formulario.domicilioSede ||
                !formulario.fechaInicio ||
                !formulario.fechaFin
              }
            >
              {guardando ? 'Guardando...' : 'Guardar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      <Dialog
        open={porDespublicar !== null}
        onClose={() => setPorDespublicar(null)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Retirar del portal</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Los registros que la gente ya adelanto se conservan.
          </Alert>
          <TextField
            label="Motivo"
            value={motivoRetiro}
            onChange={(e) => setMotivoRetiro(e.target.value)}
            fullWidth
            required
            multiline
            rows={2}
            autoFocus
            helperText="Queda registrado en la bitacora"
            inputProps={{ maxLength: 500 }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setPorDespublicar(null)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            color="warning"
            onClick={despublicar}
            disabled={!motivoRetiro.trim()}
          >
            Retirar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

/** Tarjeta de una jornada con sus acciones. */
function TarjetaJornada({
  jornada,
  destacada = false,
  tienePermiso,
  onOperar,
  onEditar,
  onPublicar,
  onDespublicar,
}: {
  jornada: Jornada;
  destacada?: boolean;
  tienePermiso: (permiso: string) => boolean;
  onOperar: () => void;
  onEditar: () => void;
  onPublicar: () => void;
  onDespublicar: () => void;
}) {
  return (
    <Card
      sx={{
        borderLeft: destacada ? 5 : 1,
        borderLeftColor: destacada ? guinda[500] : 'divider',
      }}
    >
      <CardContent>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          alignItems={{ md: 'center' }}
        >
          <Box sx={{ flexGrow: 1 }}>
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
              <Typography variant="h6">{jornada.nombre}</Typography>
              {jornada.enCurso && <Chip label="Hoy" color="success" size="small" />}
              <Chip
                label={jornada.publicada ? 'En el portal' : 'Sin publicar'}
                size="small"
                color={jornada.publicada ? 'info' : 'default'}
                variant={jornada.publicada ? 'filled' : 'outlined'}
              />
            </Stack>

            <Typography variant="body2" color="text.secondary">
              {jornada.municipio} - {jornada.domicilioSede}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {new Date(jornada.fechaInicio).toLocaleDateString('es-MX')}
              {jornada.fechaFin !== jornada.fechaInicio &&
                ` al ${new Date(jornada.fechaFin).toLocaleDateString('es-MX')}`}
              {jornada.horario ? ` - ${jornada.horario}` : ''}
              {jornada.cupo ? ` - cupo ${jornada.cupo}` : ''}
            </Typography>
          </Box>

          <Stack direction="row" spacing={1} flexWrap="wrap">
            {tienePermiso('jornada:operar') && (
              <Button
                variant={jornada.enCurso ? 'contained' : 'outlined'}
                startIcon={<PlayArrowIcon />}
                onClick={onOperar}
              >
                Operar
              </Button>
            )}

            {tienePermiso('jornada:publicar') &&
              (jornada.publicada ? (
                <Button startIcon={<PublicOffIcon />} onClick={onDespublicar} color="warning">
                  Retirar
                </Button>
              ) : (
                <Button startIcon={<PublicIcon />} onClick={onPublicar}>
                  Publicar
                </Button>
              ))}

            {tienePermiso('jornada:crear') && (
              <Button startIcon={<EditIcon />} onClick={onEditar} color="inherit">
                Editar
              </Button>
            )}
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}

export default AdminJornadas;
