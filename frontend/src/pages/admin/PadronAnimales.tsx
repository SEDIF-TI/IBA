import {
  Box,
  Card,
  Chip,
  CircularProgress,
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
  Typography,
} from '@mui/material';
import PetsIcon from '@mui/icons-material/Pets';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { padronService } from '@/services/padronService';
import type { Animal } from '@/types/dominio';

/**
 * Padron de animales.
 *
 * <p>Este es el padron propiamente dicho: lo que se empadrona es el animal. La
 * busqueda admite el numero de chip, que es como se localiza al tutor cuando
 * aparece un animal perdido.
 */
export function PadronAnimales() {
  const [animales, setAnimales] = useState<Animal[]>([]);
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
      const { resultados, mensaje: aviso } = await padronService.buscar(texto, {
        page: pagina,
        size: tamano,
      });
      setAnimales(resultados.content);
      setTotal(resultados.totalElements);
      setMensaje(aviso);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [texto, pagina, tamano]);

  // Se busca sola mientras se escribe, con una pausa para no consultar en cada
  // tecla.
  useEffect(() => {
    const temporizador = setTimeout(() => void cargar(), 400);
    return () => clearTimeout(temporizador);
  }, [cargar]);

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Padron de animales
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Perros y gatos empadronados en el estado.
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
            placeholder="Busca por nombre, identificador o numero de chip"
            helperText="Si aparece un animal perdido, busca por el numero de su chip o placa"
            sx={{ maxWidth: 520 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" />
                </InputAdornment>
              ),
            }}
            inputProps={{ 'aria-label': 'Buscar en el padron de animales' }}
          />
        </Box>
      </Card>

      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Nombre</TableCell>
                <TableCell>Especie y raza</TableCell>
                <TableCell>Rasgos</TableCell>
                <TableCell>Identificador</TableCell>
                <TableCell>Situacion</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              {cargando ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={28} aria-label="Buscando" />
                  </TableCell>
                </TableRow>
              ) : animales.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                    <PetsIcon sx={{ fontSize: 40, color: 'text.disabled' }} aria-hidden />
                    <Typography color="text.secondary" sx={{ mt: 1 }}>
                      {mensaje}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                animales.map((animal) => (
                  <TableRow
                    key={animal.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navegar(`/admin/padron/${animal.id}`)}
                  >
                    <TableCell>
                      <Typography variant="body1" fontWeight={500}>
                        {animal.nombre}
                      </Typography>
                      {animal.tutorNombre && (
                        <Typography variant="caption" color="text.secondary">
                          {animal.tutorNombre}
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">{animal.especie ?? '-'}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {animal.raza ?? 'Sin raza registrada'}
                      </Typography>
                    </TableCell>

                    <TableCell>
                      <Typography variant="body2">
                        {[animal.sexo, animal.color, animal.edadDescriptiva]
                          .filter(Boolean)
                          .join(' - ') || '-'}
                      </Typography>
                    </TableCell>

                    <TableCell>
                      <Typography variant="caption" fontFamily="monospace" display="block">
                        {animal.claveAnimal ?? '-'}
                      </Typography>
                      {animal.identificador && (
                        <Typography variant="caption" color="text.secondary">
                          {animal.metodoIdentificacion}: {animal.identificador}
                        </Typography>
                      )}
                    </TableCell>

                    <TableCell>
                      {animal.finado ? (
                        <Chip label="Finado" size="small" variant="outlined" />
                      ) : (
                        <Stack direction="row" spacing={0.5}>
                          {animal.vacunado && (
                            <Chip label="Vacunado" size="small" color="success" variant="outlined" />
                          )}
                          {animal.esterilizado && (
                            <Chip label="Esterilizado" size="small" variant="outlined" />
                          )}
                        </Stack>
                      )}
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
          labelRowsPerPage="Animales por pagina"
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
        />
      </Card>
    </Box>
  );
}

export default PadronAnimales;
