import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Stack,
  TablePagination,
  Typography,
} from '@mui/material';
import RestoreIcon from '@mui/icons-material/Restore';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import EstatusChip from '@/components/EstatusChip';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import solicitudService from '@/services/solicitudService';
import type { Solicitud } from '@/types/dominio';

/**
 * Registros dados de baja del padron.
 *
 * <p>La baja es logica precisamente para poder revisarla: si alguien dio de
 * baja por error el registro de una persona real, aqui se recupera.
 */
export function RegistrosDadosDeBaja() {
  const [registros, setRegistros] = useState<Solicitud[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [mensaje, setMensaje] = useState('');
  const [cargando, setCargando] = useState(true);

  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const { resultados, mensaje: aviso } = await solicitudService.dadosDeBaja({
        page: pagina,
        size: tamano,
      });
      setRegistros(resultados.content);
      setTotal(resultados.totalElements);
      setMensaje(aviso);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, tamano]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  async function reactivar(solicitud: Solicitud) {
    try {
      const aviso = await solicitudService.reactivar(solicitud.id);
      notificacion.exito(`${aviso}: ${solicitud.folio}`);
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Registros dados de baja
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Tramites depurados del padron. No se borraron: siguen aqui por si hubo
        un error.
      </Typography>

      {cargando ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress aria-label="Cargando" />
        </Box>
      ) : registros.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Typography color="text.secondary">{mensaje}</Typography>
          </CardContent>
        </Card>
      ) : (
        <>
          <Stack spacing={1.5}>
            {registros.map((registro) => (
              <Card key={registro.id} sx={{ opacity: 0.85 }}>
                <CardContent>
                  <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    alignItems={{ md: 'center' }}
                  >
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                      <Typography variant="body2" fontFamily="monospace">
                        {registro.folio}
                      </Typography>
                      <Typography variant="body1">
                        {registro.tutorNombre} - {registro.animalNombre}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Recibido el{' '}
                        {new Date(registro.fechaRegistro).toLocaleDateString('es-MX')} por{' '}
                        {registro.canal.toLowerCase()}
                      </Typography>
                    </Box>

                    <EstatusChip
                      clave={registro.estatusClave}
                      nombre={registro.estatusNombre}
                      color={registro.estatusColor}
                    />

                    <Stack direction="row" spacing={1}>
                      <Button
                        startIcon={<RestoreIcon />}
                        variant="outlined"
                        onClick={() => reactivar(registro)}
                      >
                        Reactivar
                      </Button>
                      <Button onClick={() => navegar(`/admin/solicitudes/${registro.id}`)}>
                        Ver
                      </Button>
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>

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
            labelRowsPerPage="Registros por página"
            labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
          />
        </>
      )}

      <Alert severity="info" sx={{ mt: 2 }}>
        Cada baja quedo registrada en la bitacora con quien la hizo y por que.
      </Alert>
    </Box>
  );
}

export default RegistrosDadosDeBaja;
