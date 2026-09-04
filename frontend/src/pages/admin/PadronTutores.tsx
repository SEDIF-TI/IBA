import {
  Alert,
  Box,
  Card,
  Chip,
  CircularProgress,
  InputAdornment,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { tutorService } from '@/services/padronService';
import type { Tutor } from '@/types/dominio';

/**
 * Tutores responsables.
 *
 * <p>La persona no se empadrona: el padron es de animales. Esta pantalla sirve
 * para localizar a quien responde por ellos, sobre todo cuando alguien llega
 * preguntando por sus tramites.
 *
 * <p>La CURP va enmascarada en el listado. Se ve completa solo al abrir la
 * ficha, y ese acceso queda registrado.
 */
export function PadronTutores() {
  const [tutores, setTutores] = useState<Tutor[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [texto, setTexto] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [cargando, setCargando] = useState(true);

  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const { resultados, mensaje: aviso } = await tutorService.buscar(texto, {
        page: pagina,
        size: tamano,
      });
      setTutores(resultados.content);
      setTotal(resultados.totalElements);
      setMensaje(aviso);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [texto, pagina, tamano]);

  useEffect(() => {
    const temporizador = setTimeout(() => void cargar(), 400);
    return () => clearTimeout(temporizador);
  }, [cargar]);

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Tutores responsables
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Personas que responden por los animales empadronados.
      </Typography>

      <Card sx={{ mb: 2 }}>
        <Box sx={{ p: 2 }}>
          <TextField
            value={texto}
            onChange={(e) => {
              setTexto(e.target.value);
              setPagina(0);
            }}
            fullWidth
            autoFocus
            placeholder="Busca por apellidos, nombre o CURP"
            helperText="Puedes escribir sin acentos: 'munoz' encuentra 'Muñoz'"
            sx={{ maxWidth: 520 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" />
                </InputAdornment>
              ),
            }}
            inputProps={{ 'aria-label': 'Buscar tutor responsable' }}
          />
        </Box>
      </Card>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Nombre</TableCell>
                <TableCell>CURP</TableCell>
                <TableCell>Contacto</TableCell>
                <TableCell>Municipio</TableCell>
                <TableCell align="center">Animales</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {cargando ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} aria-label="Buscando" />
                  </TableCell>
                </TableRow>
              ) : tutores.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                    <PeopleIcon sx={{ fontSize: 40, color: 'text.disabled' }} aria-hidden />
                    <Typography color="text.secondary" sx={{ mt: 1 }}>
                      {mensaje}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                tutores.map((tutor) => (
                  <TableRow
                    key={tutor.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navegar(`/admin/tutores/${tutor.id}`)}
                  >
                    <TableCell>
                      <Typography variant="body1" fontWeight={500}>
                        {tutor.nombreCompleto}
                      </Typography>
                    </TableCell>

                    <TableCell>
                      {tutor.curpEnmascarada ? (
                        <Typography variant="body2" fontFamily="monospace">
                          {tutor.curpEnmascarada}
                        </Typography>
                      ) : (
                        <Chip label="Sin CURP" size="small" color="warning" variant="outlined" />
                      )}
                      {tutor.curpEnmascarada && !tutor.identidadValidada && (
                        <Typography variant="caption" color="warning.main" display="block">
                          Sin cotejar
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">{tutor.telefono ?? '-'}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {tutor.correo ?? 'Sin correo'}
                      </Typography>
                    </TableCell>

                    <TableCell>{tutor.municipio ?? '-'}</TableCell>

                    <TableCell align="center">
                      <Chip
                        label={tutor.animalesACargo}
                        size="small"
                        color={tutor.animalesACargo > 0 ? 'primary' : 'default'}
                      />
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
          labelRowsPerPage="Tutores por pagina"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
        />
      </Card>

      <Alert severity="info" sx={{ mt: 2 }}>
        La CURP se muestra enmascarada. Se ve completa al abrir la ficha, y ese
        acceso queda registrado en la bitacora.
      </Alert>
    </Box>
  );
}

export default PadronTutores;
