import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Stack,
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
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import RestoreIcon from '@mui/icons-material/Restore';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import ConfirmDialog from '@/components/ConfirmDialog';
import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { soloDigitos } from '@/utils/validaciones';
import { mensajeDeError } from '@/services/api';
import { crearCatalogoService } from '@/services/catalogoService';
import type { Catalogo, CatalogoRequest } from '@/types/dominio';

interface Props {
  /** Ruta del catalogo en el API, por ejemplo "colores". */
  ruta: string;
  /** Titulo de la pantalla. */
  titulo: string;
  /** Nombre en singular, para los mensajes. */
  singular: string;
}

const FORMULARIO_VACIO: CatalogoRequest = {
  clave: '',
  nombre: '',
  descripcion: '',
  orden: 0,
};

/**
 * Pantalla de administracion de un catalogo.
 *
 * <p>Sirve para los trece: todos se comportan igual, asi que en lugar de trece
 * pantallas calcadas hay una que recibe la ruta. Esta es la que conviene copiar
 * al crear una pantalla nueva.
 */
export function AdminCatalogo({ ruta, titulo, singular }: Props) {
  const [registros, setRegistros] = useState<Catalogo[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [texto, setTexto] = useState('');
  const [cargando, setCargando] = useState(true);

  const [formularioAbierto, setFormularioAbierto] = useState(false);
  const [editando, setEditando] = useState<Catalogo | null>(null);
  const [formulario, setFormulario] = useState<CatalogoRequest>(FORMULARIO_VACIO);
  const [guardando, setGuardando] = useState(false);

  const [porDesactivar, setPorDesactivar] = useState<Catalogo | null>(null);

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const servicio = crearCatalogoService(ruta);

  const puedeAdministrar = tienePermiso('catalogo:administrar');

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const respuesta = await servicio.listar({
        page: pagina,
        size: tamano,
        texto: texto || undefined,
      });
      setRegistros(respuesta.content);
      setTotal(respuesta.totalElements);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // El servicio se construye en cada render pero es equivalente; lo que
    // importa para recargar es la ruta, la pagina, el tamano y el filtro.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ruta, pagina, tamano, texto]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function abrirAlta() {
    setEditando(null);
    setFormulario(FORMULARIO_VACIO);
    setFormularioAbierto(true);
  }

  function abrirEdicion(registro: Catalogo) {
    setEditando(registro);
    setFormulario({
      clave: registro.clave,
      nombre: registro.nombre,
      descripcion: registro.descripcion ?? '',
      orden: registro.orden,
    });
    setFormularioAbierto(true);
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault();
    setGuardando(true);

    try {
      if (editando) {
        await servicio.actualizar(editando.id, { ...formulario, version: editando.version });
        notificacion.exito(`Se guardaron los cambios de "${formulario.nombre}"`);
      } else {
        await servicio.crear(formulario);
        notificacion.exito(
          `Se agrego "${formulario.nombre}" y ya aparece en los formularios`,
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

  async function desactivar() {
    if (!porDesactivar) return;
    try {
      await servicio.desactivar(porDesactivar.id);
      notificacion.exito(
        `"${porDesactivar.nombre}" dejo de ofrecerse en los formularios. ` +
          'Los registros que ya lo usan siguen intactos.',
      );
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setPorDesactivar(null);
    }
  }

  async function reactivar(registro: Catalogo) {
    try {
      await servicio.reactivar(registro.id);
      notificacion.exito(`"${registro.nombre}" vuelve a estar disponible`);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  return (
    <Box>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
        spacing={2}
        sx={{ mb: 3 }}
      >
        <Typography variant="h5" fontWeight={600}>
          {titulo}
        </Typography>

        {puedeAdministrar && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
            Agregar
          </Button>
        )}
      </Stack>

      <Card>
        <Box sx={{ p: 2 }}>
          <TextField
            placeholder="Buscar por clave o nombre"
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              setPagina(0);
            }}
            size="small"
            fullWidth
            sx={{ maxWidth: 360 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
            inputProps={{ 'aria-label': `Buscar en ${titulo}` }}
          />
        </Box>

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Clave</TableCell>
                <TableCell>Nombre</TableCell>
                <TableCell>Descripción</TableCell>
                <TableCell align="center">Orden</TableCell>
                <TableCell align="center">Situación</TableCell>
                {puedeAdministrar && <TableCell align="right">Acciones</TableCell>}
              </TableRow>
            </TableHead>

            <TableBody>
              {cargando ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} aria-label="Cargando" />
                  </TableCell>
                </TableRow>
              ) : registros.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6 }}>
                    <Typography color="text.secondary">
                      {texto
                        ? 'No se encontraron registros con ese texto'
                        : 'Todavia no hay registros'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                registros.map((registro) => (
                  <TableRow key={registro.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontFamily="monospace">
                        {registro.clave}
                      </Typography>
                    </TableCell>
                    <TableCell>{registro.nombre}</TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {registro.descripcion ?? '-'}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">{registro.orden}</TableCell>
                    <TableCell align="center">
                      {/* El texto acompana al color: quien no lo distingue
                          tiene que poder leerlo igual. */}
                      <Chip
                        label={registro.activo ? 'Disponible' : 'Dado de baja'}
                        size="small"
                        color={registro.activo ? 'success' : 'default'}
                        variant={registro.activo ? 'filled' : 'outlined'}
                      />
                    </TableCell>

                    {puedeAdministrar && (
                      <TableCell align="right">
                        <Tooltip title="Editar">
                          <IconButton
                            size="small"
                            onClick={() => abrirEdicion(registro)}
                            aria-label={`Editar ${registro.nombre}`}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        {registro.activo ? (
                          <Tooltip title="Dar de baja">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => setPorDesactivar(registro)}
                              aria-label={`Dar de baja ${registro.nombre}`}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        ) : (
                          <Tooltip title="Reactivar">
                            <IconButton
                              size="small"
                              color="success"
                              onClick={() => reactivar(registro)}
                              aria-label={`Reactivar ${registro.nombre}`}
                            >
                              <RestoreIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
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
          rowsPerPageOptions={[10, 20, 50, 100]}
          labelRowsPerPage="Registros por página"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
        />
      </Card>

      <Dialog
        open={formularioAbierto}
        onClose={() => setFormularioAbierto(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={guardar}>
          <DialogTitle>
            {editando ? `Editar ${singular}` : `Agregar ${singular}`}
          </DialogTitle>

          <DialogContent>
            <TextField
              label="Clave"
              value={formulario.clave}
              onChange={(e) =>
                setFormulario({ ...formulario, clave: e.target.value.toUpperCase() })
              }
              fullWidth
              required
              margin="normal"
              // La clave no se cambia al editar: el codigo la usa como
              // referencia estable y cambiarla romperia lo que dependa de ella.
              disabled={editando !== null}
              helperText={
                editando
                  ? 'La clave no se puede modificar'
                  : 'Mayusculas, numeros y guion bajo'
              }
              inputProps={{ pattern: '[A-Z0-9_]+', maxLength: 50 }}
            />

            <TextField
              label="Nombre"
              value={formulario.nombre}
              onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })}
              fullWidth
              required
              margin="normal"
              inputProps={{ maxLength: 150 }}
            />

            <TextField
              label="Descripción"
              value={formulario.descripcion}
              onChange={(e) =>
                setFormulario({ ...formulario, descripcion: e.target.value })
              }
              fullWidth
              multiline
              rows={2}
              margin="normal"
              inputProps={{ maxLength: 500 }}
            />

            <TextField
              label="Orden"
              value={formulario.orden ?? ''}
              // Solo acepta digitos al escribir (nunca type="number": admite e, + y - y cambia
              // el valor con la rueda del raton).
              inputProps={{ inputMode: 'numeric', maxLength: 4 }}
              onChange={(e) => {
                const digitos = soloDigitos(e.target.value, 4);
                setFormulario({ ...formulario, orden: digitos ? parseInt(digitos, 10) : 0 });
              }}
              fullWidth
              margin="normal"
              helperText="Posicion en la que aparece en las listas"
            />
          </DialogContent>

          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button onClick={() => setFormularioAbierto(false)} color="inherit">
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={guardando || !formulario.clave || !formulario.nombre}
            >
              {guardando ? 'Guardando...' : 'Guardar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      <ConfirmDialog
        abierto={porDesactivar !== null}
        titulo={`Dar de baja ${singular}`}
        mensaje={
          `Se dara de baja "${porDesactivar?.nombre}". Dejara de ofrecerse en los ` +
          'formularios, pero los registros que ya lo usan siguen intactos.'
        }
        textoConfirmar="Dar de baja"
        peligroso
        onConfirmar={desactivar}
        onCancelar={() => setPorDesactivar(null)}
      />
    </Box>
  );
}

export default AdminCatalogo;
