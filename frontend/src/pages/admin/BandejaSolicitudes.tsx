import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
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
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import PriorityHighIcon from '@mui/icons-material/PriorityHigh';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import EstatusChip from '@/components/EstatusChip';
import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { catalogos } from '@/services/catalogoService';
import solicitudService, { type FiltrosBandeja } from '@/services/solicitudService';
import type { Catalogo, Solicitud } from '@/types/dominio';

/**
 * Bandeja de trabajo del personal.
 *
 * <p>Es la pantalla donde se pasa el dia: llegan los tramites, se revisan y se
 * resuelven. Por eso el filtro por situacion esta a la vista y lo marcado para
 * revision reforzada se distingue de un vistazo.
 */
export function BandejaSolicitudes() {
  const [solicitudes, setSolicitudes] = useState<Solicitud[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [cargando, setCargando] = useState(true);

  const [estatus, setEstatus] = useState<Catalogo[]>([]);
  const [municipios, setMunicipios] = useState<Catalogo[]>([]);

  const [filtroEstatus, setFiltroEstatus] = useState<number | ''>('');
  const [filtroMunicipio, setFiltroMunicipio] = useState<number | ''>('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  // Los catalogos de los filtros se cargan una vez: cambian muy rara vez.
  useEffect(() => {
    async function cargarCatalogos() {
      try {
        const [listaEstatus, listaMunicipios] = await Promise.all([
          catalogos.estatusSolicitud.opciones(),
          catalogos.municipios.opciones(),
        ]);
        setEstatus(listaEstatus);
        setMunicipios(listaMunicipios);
      } catch (fallo) {
        notificacion.error(mensajeDeError(fallo));
      }
    }
    void cargarCatalogos();
    // Solo al montar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const filtros: FiltrosBandeja = {
        page: pagina,
        size: tamano,
        estatusId: filtroEstatus === '' ? undefined : filtroEstatus,
        municipioId: filtroMunicipio === '' ? undefined : filtroMunicipio,
        // El servidor espera fecha y hora; se completa el extremo del dia para
        // que "hasta el 5" incluya todo el dia 5 y no se corte a medianoche.
        desde: desde ? `${desde}T00:00:00-06:00` : undefined,
        hasta: hasta ? `${hasta}T23:59:59-06:00` : undefined,
      };

      const respuesta = await solicitudService.listar(filtros);
      setSolicitudes(respuesta.content);
      setTotal(respuesta.totalElements);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, tamano, filtroEstatus, filtroMunicipio, desde, hasta]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function limpiarFiltros() {
    setFiltroEstatus('');
    setFiltroMunicipio('');
    setDesde('');
    setHasta('');
    setPagina(0);
  }

  const hayFiltros =
    filtroEstatus !== '' || filtroMunicipio !== '' || desde !== '' || hasta !== '';

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
            Trámites
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Solicitudes recibidas del portal, de ventanilla y de las jornadas.
          </Typography>
        </Box>

        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
          {tienePermiso('solicitud:depurar') && (
            <Button
              color="inherit"
              startIcon={<DeleteOutlineIcon />}
              onClick={() => navegar('/admin/solicitudes/dados-de-baja')}
            >
              Dados de baja
            </Button>
          )}
          {tienePermiso('solicitud:crear') && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => navegar('/admin/solicitudes/nueva')}
            >
              Capturar en ventanilla
            </Button>
          )}
        </Stack>
      </Stack>

      <Card sx={{ mb: 2 }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          spacing={2}
          sx={{ p: 2 }}
          alignItems={{ md: 'center' }}
        >
          <FormControl size="small" sx={{ minWidth: 190 }}>
            <InputLabel id="etiqueta-situacion">Situación</InputLabel>
            <Select
              labelId="etiqueta-situacion"
              label="Situación"
              value={filtroEstatus}
              onChange={(e) => {
                setFiltroEstatus(e.target.value === '' ? '' : Number(e.target.value));
                setPagina(0);
              }}
            >
              <MenuItem value="">Todas</MenuItem>
              {estatus.map((situacion) => (
                <MenuItem key={situacion.id} value={situacion.id}>
                  {situacion.nombre}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 190 }}>
            <InputLabel id="etiqueta-municipio">Municipio</InputLabel>
            <Select
              labelId="etiqueta-municipio"
              label="Municipio"
              value={filtroMunicipio}
              onChange={(e) => {
                setFiltroMunicipio(e.target.value === '' ? '' : Number(e.target.value));
                setPagina(0);
              }}
            >
              <MenuItem value="">Todos</MenuItem>
              {municipios.map((municipio) => (
                <MenuItem key={municipio.id} value={municipio.id}>
                  {municipio.nombre}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            label="Desde"
            type="date"
            size="small"
            value={desde}
            onChange={(e) => {
              setDesde(e.target.value);
              setPagina(0);
            }}
            InputLabelProps={{ shrink: true }}
          />

          <TextField
            label="Hasta"
            type="date"
            size="small"
            value={hasta}
            onChange={(e) => {
              setHasta(e.target.value);
              setPagina(0);
            }}
            InputLabelProps={{ shrink: true }}
          />

          {hayFiltros && (
            <Button onClick={limpiarFiltros} size="small" color="inherit">
              Limpiar
            </Button>
          )}
        </Stack>
      </Card>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Folio</TableCell>
                <TableCell>Tutor</TableCell>
                <TableCell>Animal</TableCell>
                <TableCell>Municipio</TableCell>
                <TableCell>Canal</TableCell>
                <TableCell>Situación</TableCell>
                <TableCell>Recibido</TableCell>
                <TableCell align="right">Acciones</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {cargando ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} aria-label="Cargando trámites" />
                  </TableCell>
                </TableRow>
              ) : solicitudes.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                    <Typography color="text.secondary">
                      {hayFiltros
                        ? 'Ningún trámite coincide con los filtros'
                        : 'Todavia no hay trámites'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                solicitudes.map((solicitud) => (
                  <TableRow
                    key={solicitud.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navegar(`/admin/solicitudes/${solicitud.id}`)}
                  >
                    <TableCell>
                      <Stack direction="row" spacing={0.5} alignItems="center">
                        {/* Lo marcado para revision reforzada se distingue sin
                            tener que abrirlo. */}
                        {solicitud.revisionReforzada && (
                          <Tooltip title={solicitud.motivoRevisionReforzada ?? 'Revisión reforzada'}>
                            <PriorityHighIcon color="warning" fontSize="small" />
                          </Tooltip>
                        )}
                        <Typography variant="body2" fontFamily="monospace">
                          {solicitud.folio}
                        </Typography>
                      </Stack>
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">{solicitud.tutorNombre}</Typography>
                      {solicitud.tutorCurpEnmascarada && (
                        <Typography variant="caption" color="text.secondary" fontFamily="monospace">
                          {solicitud.tutorCurpEnmascarada}
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">{solicitud.animalNombre}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {solicitud.animalEspecie}
                      </Typography>
                    </TableCell>

                    <TableCell>{solicitud.tutorMunicipio ?? '-'}</TableCell>

                    <TableCell>
                      <Chip label={solicitud.canal} size="small" variant="outlined" />
                    </TableCell>

                    <TableCell>
                      <EstatusChip
                        clave={solicitud.estatusClave}
                        nombre={solicitud.estatusNombre}
                        color={solicitud.estatusColor}
                      />
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">
                        {new Date(solicitud.fechaRegistro).toLocaleDateString('es-MX')}
                      </Typography>
                    </TableCell>

                    <TableCell align="right">
                      <Tooltip title="Ver detalle">
                        <Button
                          size="small"
                          startIcon={<VisibilityIcon />}
                          onClick={(evento) => {
                            evento.stopPropagation();
                            navegar(`/admin/solicitudes/${solicitud.id}`);
                          }}
                        >
                          Abrir
                        </Button>
                      </Tooltip>
                    </TableCell>
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
          labelRowsPerPage="Trámites por página"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
        />
      </Card>
    </Box>
  );
}

export default BandejaSolicitudes;
