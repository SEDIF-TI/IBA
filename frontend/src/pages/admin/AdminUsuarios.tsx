import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  IconButton,
  InputAdornment,
  Autocomplete,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import BadgeIcon from '@mui/icons-material/Badge';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import EditIcon from '@mui/icons-material/Edit';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import SearchIcon from '@mui/icons-material/Search';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { useCallback, useEffect, useState } from 'react';

import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import usuarioService, {
  type AltaRespuesta,
  type GuardarUsuarioRequest,
  type RolResumen,
  type Usuario,
} from '@/services/usuarioService';
import { soloDigitos, validarCorreo, validarNombre, validarTelefono } from '@/utils/validaciones';

const FORMULARIO_VACIO: GuardarUsuarioRequest = {
  usuario: '',
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  correo: '',
  telefono: '',
  rolesIds: [],
};

/**
 * Cuentas del personal.
 *
 * <p>No hay autoservicio: las cuentas las crea informatica, porque una cuenta
 * decide sobre tramites de terceros y debe existir alguien que responda por
 * haberla dado de alta.
 *
 * <p>La contrasena nunca la elige quien crea la cuenta. El sistema genera una
 * temporal, se muestra una sola vez y la persona debe cambiarla al entrar.
 */
export function AdminUsuarios() {
  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();

  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [roles, setRoles] = useState<RolResumen[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [texto, setTexto] = useState('');
  const [soloActivos, setSoloActivos] = useState(true);
  const [cargando, setCargando] = useState(true);
  const [aviso, setAviso] = useState('');

  const [formulario, setFormulario] = useState<GuardarUsuarioRequest>(FORMULARIO_VACIO);
  const [editando, setEditando] = useState<Usuario | null>(null);
  const [abierto, setAbierto] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [credencial, setCredencial] = useState<AltaRespuesta | null>(null);

  const puedeAdministrar = tienePermiso('usuario:administrar');

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const { pagina: resultado, mensaje } = await usuarioService.buscar(texto, {
        page: pagina,
        size: tamano,
        soloActivos,
      });
      setUsuarios(resultado.content);
      setTotal(resultado.totalElements);
      setAviso(resultado.content.length === 0 ? mensaje : '');
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [texto, pagina, tamano, soloActivos]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  useEffect(() => {
    usuarioService
      .roles()
      .then(setRoles)
      .catch((fallo) => notificacion.error(mensajeDeError(fallo)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function abrirAlta() {
    setEditando(null);
    setFormulario(FORMULARIO_VACIO);
    setAbierto(true);
  }

  function abrirEdicion(usuario: Usuario) {
    setEditando(usuario);
    setFormulario({
      usuario: usuario.usuario,
      nombre: usuario.nombre,
      apellidoPaterno: usuario.apellidoPaterno,
      apellidoMaterno: usuario.apellidoMaterno ?? '',
      correo: usuario.correo,
      telefono: usuario.telefono ?? '',
      sedeId: usuario.sedeId,
      rolesIds: usuario.roles.map((rol) => rol.id),
    });
    setAbierto(true);
  }

  const vNombre = validarNombre(formulario.nombre, 'nombre');
  const vPaterno = validarNombre(formulario.apellidoPaterno, 'apellido paterno');
  const vCorreo = validarCorreo(formulario.correo, true);
  const vTelefono = validarTelefono(formulario.telefono ?? '');
  const usuarioValido =
    formulario.usuario.length >= 4 && /^[a-zA-Z0-9._-]+$/.test(formulario.usuario);

  const formularioListo =
    usuarioValido &&
    !vNombre.error &&
    !vPaterno.error &&
    !vCorreo.error &&
    !vTelefono.error &&
    formulario.rolesIds.length > 0;

  async function guardar() {
    setGuardando(true);
    try {
      if (editando) {
        const { mensaje } = await usuarioService.actualizar(editando.id, {
          ...formulario,
          version: editando.version,
        });
        notificacion.exito(mensaje);
        setAbierto(false);
      } else {
        const { alta, mensaje } = await usuarioService.crear(formulario);
        notificacion.exito(mensaje);
        setAbierto(false);
        setCredencial(alta);
      }
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setGuardando(false);
    }
  }

  async function reiniciar(usuario: Usuario) {
    try {
      const { alta, mensaje } = await usuarioService.reiniciarPassword(usuario.id);
      notificacion.exito(mensaje);
      setCredencial(alta);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  async function cambiarVigencia(usuario: Usuario) {
    try {
      const { mensaje } = await usuarioService.cambiarVigencia(usuario.id, !usuario.activo);
      notificacion.exito(mensaje);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  async function desbloquear(usuario: Usuario) {
    try {
      const { mensaje } = await usuarioService.desbloquear(usuario.id);
      notificacion.exito(mensaje);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  return (
    <Box>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        alignItems={{ sm: 'center' }}
        sx={{ mb: 3 }}
      >
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" fontWeight={600}>
            Personal del sistema
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Cuentas que pueden entrar al panel administrativo.
          </Typography>
        </Box>

        {puedeAdministrar && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
            Dar de alta
          </Button>
        )}
      </Stack>

      {!puedeAdministrar && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Puedes consultar quién tiene acceso, pero dar de alta o modificar cuentas
          le corresponde a informática.
        </Alert>
      )}

      <Card sx={{ mb: 2, p: 2 }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
          <TextField
            fullWidth
            size="small"
            placeholder="Buscar por nombre, usuario o correo"
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              setPagina(0);
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={soloActivos}
                onChange={(e) => {
                  setSoloActivos(e.target.checked);
                  setPagina(0);
                }}
              />
            }
            label="Solo activas"
            sx={{ whiteSpace: 'nowrap' }}
          />
        </Stack>
      </Card>

      <Card>
        {cargando ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : aviso ? (
          <Alert severity="info" sx={{ m: 2 }}>
            {aviso}
          </Alert>
        ) : (
          <>
            <TableContainer sx={{ overflowX: 'auto' }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Persona</TableCell>
                    <TableCell>Usuario</TableCell>
                    <TableCell>Correo</TableCell>
                    <TableCell>Rol</TableCell>
                    <TableCell>Estado</TableCell>
                    {puedeAdministrar && <TableCell align="right">Acciones</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {usuarios.map((usuario) => (
                    <TableRow key={usuario.id} hover>
                      <TableCell>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <BadgeIcon fontSize="small" color="action" />
                          <span>{usuario.nombreCompleto}</span>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <code>{usuario.usuario}</code>
                      </TableCell>
                      <TableCell>{usuario.correo}</TableCell>
                      <TableCell>
                        {usuario.roles.map((rol) => (
                          <Chip
                            key={rol.id}
                            label={rol.nombre}
                            size="small"
                            sx={{ mr: 0.5 }}
                          />
                        ))}
                      </TableCell>
                      <TableCell>
                        {!usuario.activo && (
                          <Chip label="Dada de baja" size="small" color="default" />
                        )}
                        {usuario.activo && usuario.bloqueada && (
                          <Chip label="Bloqueada" size="small" color="error" />
                        )}
                        {usuario.activo && !usuario.bloqueada && (
                          <Chip
                            label="Activa"
                            size="small"
                            color="success"
                            variant="outlined"
                          />
                        )}
                        {usuario.cambioPasswordRequerido && usuario.activo && (
                          <Chip
                            label="Debe cambiar contraseña"
                            size="small"
                            color="warning"
                            variant="outlined"
                            sx={{ ml: 0.5 }}
                          />
                        )}
                      </TableCell>

                      {puedeAdministrar && (
                        <TableCell align="right">
                          <Tooltip title="Editar datos y roles">
                            <IconButton
                              size="small"
                              onClick={() => abrirEdicion(usuario)}
                              aria-label={`Editar ${usuario.usuario}`}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          <Tooltip title="Generar contraseña nueva">
                            <IconButton
                              size="small"
                              onClick={() => void reiniciar(usuario)}
                              aria-label={`Reiniciar contraseña de ${usuario.usuario}`}
                            >
                              <VpnKeyIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>

                          {usuario.bloqueada && (
                            <Tooltip title="Desbloquear">
                              <IconButton
                                size="small"
                                color="warning"
                                onClick={() => void desbloquear(usuario)}
                                aria-label={`Desbloquear ${usuario.usuario}`}
                              >
                                <LockOpenIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}

                          <Button
                            size="small"
                            color={usuario.activo ? 'error' : 'success'}
                            onClick={() => void cambiarVigencia(usuario)}
                          >
                            {usuario.activo ? 'Dar de baja' : 'Reactivar'}
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            <TablePagination
              component="div"
              count={total}
              page={pagina}
              onPageChange={(_, nueva) => setPagina(nueva)}
              rowsPerPage={tamano}
              onRowsPerPageChange={(e) => {
                setTamano(parseInt(e.target.value, 10));
                setPagina(0);
              }}
              rowsPerPageOptions={[10, 20, 50]}
              labelRowsPerPage="Por página"
            />
          </>
        )}
      </Card>

      {/* Alta y edicion */}
      <Dialog open={abierto} onClose={() => setAbierto(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editando ? 'Editar cuenta' : 'Dar de alta una cuenta'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Usuario"
              value={formulario.usuario}
              onChange={(e) =>
                setFormulario({ ...formulario, usuario: e.target.value.trim() })
              }
              disabled={!!editando}
              helperText={
                editando
                  ? 'El usuario no se puede cambiar: identifica lo que ya resolvió.'
                  : 'Con esto entrará al sistema. Letras, números, punto, guion y guion bajo.'
              }
              error={formulario.usuario.length > 0 && !usuarioValido}
              fullWidth
              size="small"
            />

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Nombre"
                value={formulario.nombre}
                onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
                error={!!vNombre.error}
                helperText={vNombre.error}
                fullWidth
                size="small"
              />
              <TextField
                label="Apellido paterno"
                value={formulario.apellidoPaterno}
                onChange={(e) =>
                  setFormulario({ ...formulario, apellidoPaterno: e.target.value })
                }
                error={!!vPaterno.error}
                helperText={vPaterno.error}
                fullWidth
                size="small"
              />
            </Stack>

            <TextField
              label="Apellido materno"
              value={formulario.apellidoMaterno}
              onChange={(e) =>
                setFormulario({ ...formulario, apellidoMaterno: e.target.value })
              }
              fullWidth
              size="small"
            />

            <TextField
              label="Correo institucional"
              value={formulario.correo}
              onChange={(e) => setFormulario({ ...formulario, correo: e.target.value })}
              error={!!vCorreo.error}
              helperText={vCorreo.error || 'Por aquí se avisa cualquier asunto de la cuenta.'}
              fullWidth
              size="small"
            />

            <TextField
              label="Teléfono"
              value={formulario.telefono}
              onChange={(e) =>
                setFormulario({ ...formulario, telefono: soloDigitos(e.target.value, 10) })
              }
              error={Boolean(vTelefono.error)}
              helperText={vTelefono.error ?? 'Opcional. 10 dígitos.'}
              inputProps={{ inputMode: 'numeric' }}
              fullWidth
              size="small"
            />

            {/* Lista de autocompletado (regla del equipo: nunca TextField select). */}
            <Autocomplete
              options={roles}
              getOptionLabel={(rol) => rol.nombre}
              isOptionEqualToValue={(opcion, valor) => opcion.id === valor.id}
              value={roles.find((rol) => rol.id === formulario.rolesIds[0]) ?? null}
              onChange={(_evento, rol) =>
                setFormulario({ ...formulario, rolesIds: rol ? [rol.id] : [] })
              }
              noOptionsText="No hay roles que coincidan"
              fullWidth
              size="small"
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Rol"
                  helperText="Determina qué puede hacer dentro del sistema."
                />
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAbierto(false)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            onClick={() => void guardar()}
            disabled={!formularioListo || guardando}
          >
            {guardando ? 'Guardando...' : editando ? 'Guardar cambios' : 'Crear cuenta'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Contrasena temporal: se muestra una sola vez */}
      <Dialog open={!!credencial} onClose={() => setCredencial(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Contraseña temporal</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Entrégasela a <strong>{credencial?.usuario.nombreCompleto}</strong>. Es la
            única vez que se muestra: no se guarda y no se puede volver a consultar.
          </DialogContentText>

          <Box
            sx={{
              p: 2,
              bgcolor: 'grey.100',
              borderRadius: 1,
              display: 'flex',
              alignItems: 'center',
              gap: 1,
            }}
          >
            <Typography variant="h6" fontFamily="monospace" sx={{ flexGrow: 1 }}>
              {credencial?.passwordTemporal}
            </Typography>
            <Tooltip title="Copiar">
              <IconButton
                size="small"
                onClick={() => {
                  void navigator.clipboard.writeText(credencial?.passwordTemporal ?? '');
                  notificacion.exito('Contraseña copiada.');
                }}
                aria-label="Copiar contraseña"
              >
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>

          <Alert severity="info" sx={{ mt: 2 }}>
            Al entrar, el sistema le pedirá cambiarla por una suya.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={() => setCredencial(null)}>
            Ya la entregué
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default AdminUsuarios;
