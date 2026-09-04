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
  Stack,
  TablePagination,
  TextField,
  Typography,
} from '@mui/material';
import PhoneIcon from '@mui/icons-material/Phone';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import notificacionService from '@/services/notificacionService';
import { guinda } from '@/theme';
import type { Notificacion } from '@/types/dominio';

/**
 * Bandeja de contacto telefonico.
 *
 * <p>Reune a las personas que solo dejaron telefono, asi que su aviso no puede
 * salir solo. Sin esta pantalla esos avisos no ocurririan y nadie se enteraria
 * de que hay gente esperando respuesta: era justo lo que pasaba antes.
 *
 * <p>Se ordena del mas antiguo al mas reciente, para que nadie quede olvidado
 * al final de la lista.
 */
export function BandejaContacto() {
  const [contactos, setContactos] = useState<Notificacion[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [envioHabilitado, setEnvioHabilitado] = useState(true);
  const [cargando, setCargando] = useState(true);

  const [atendiendo, setAtendiendo] = useState<Notificacion | null>(null);
  const [observacion, setObservacion] = useState('');
  const [registrando, setRegistrando] = useState(false);

  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [pendientes, resumen] = await Promise.all([
        notificacionService.bandeja({ page: pagina, size: tamano }),
        notificacionService.pendientes(),
      ]);
      setContactos(pendientes.content);
      setTotal(pendientes.totalElements);
      setEnvioHabilitado(resumen.envioHabilitado);
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

  async function registrar(logrado: boolean) {
    if (!atendiendo) return;

    setRegistrando(true);
    try {
      const { mensaje } = await notificacionService.registrarContacto(
        atendiendo.id,
        logrado,
        observacion,
      );
      notificacion.exito(mensaje);
      cerrar();
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setRegistrando(false);
    }
  }

  function cerrar() {
    setAtendiendo(null);
    setObservacion('');
  }

  async function reintentarCorreos() {
    try {
      const { mensaje } = await notificacionService.reintentar();
      notificacion.info(mensaje);
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
        sx={{ mb: 2 }}
      >
        <Box>
          <Typography variant="h5" fontWeight={600}>
            Bandeja de contacto
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Personas que solo dejaron telefono: hay que avisarles por llamada o
            mensaje.
          </Typography>
        </Box>

        <Button startIcon={<RefreshIcon />} onClick={reintentarCorreos}>
          Reintentar correos
        </Button>
      </Stack>

      {!envioHabilitado && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <Typography variant="subtitle2">
            El envio de correo no esta configurado
          </Typography>
          <Typography variant="body2">
            Ningun aviso sale por correo, ni siquiera a quienes lo dejaron. Todos
            quedan aqui para atenderlos por telefono. Pide a informatica que
            configure el servidor de correo.
          </Typography>
        </Alert>
      )}

      {cargando ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress aria-label="Cargando la bandeja" />
        </Box>
      ) : contactos.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Typography variant="h6" color="success.main" gutterBottom>
              No hay contactos pendientes
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Todas las personas que requerian aviso por telefono ya fueron
              atendidas.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {total} persona(s) por contactar, de la mas antigua a la mas reciente
          </Typography>

          <Stack spacing={1.5}>
            {contactos.map((contacto) => {
              const diasEspera = Math.floor(
                (Date.now() - new Date(contacto.fechaCreacion).getTime()) / 86_400_000,
              );

              return (
                <Card
                  key={contacto.id}
                  sx={{
                    borderLeft: 4,
                    // Lo que lleva mas de tres dias esperando se distingue.
                    borderLeftColor: diasEspera >= 3 ? 'warning.main' : guinda[300],
                  }}
                >
                  <CardContent>
                    <Stack
                      direction={{ xs: 'column', md: 'row' }}
                      spacing={2}
                      alignItems={{ md: 'center' }}
                    >
                      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Typography variant="h6">{contacto.tutorNombre}</Typography>
                          {diasEspera >= 3 && (
                            <Chip
                              label={`${diasEspera} dias esperando`}
                              size="small"
                              color="warning"
                            />
                          )}
                        </Stack>

                        <Stack direction="row" spacing={0.5} alignItems="center">
                          <PhoneIcon fontSize="small" color="action" />
                          <Typography variant="h6" fontFamily="monospace">
                            {contacto.destinatario}
                          </Typography>
                        </Stack>

                        <Typography variant="body2" color="text.secondary">
                          {contacto.asunto}
                        </Typography>
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          fontFamily="monospace"
                        >
                          {contacto.folioSolicitud}
                        </Typography>
                      </Box>

                      <Stack direction="row" spacing={1}>
                        <Button
                          variant="contained"
                          size="large"
                          startIcon={<PhoneIcon />}
                          onClick={() => setAtendiendo(contacto)}
                        >
                          Registrar llamada
                        </Button>
                        <Button
                          size="large"
                          onClick={() =>
                            navegar(`/admin/solicitudes?folio=${contacto.folioSolicitud}`)
                          }
                        >
                          Ver tramite
                        </Button>
                      </Stack>
                    </Stack>
                  </CardContent>
                </Card>
              );
            })}
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
            labelRowsPerPage="Contactos por página"
            labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
          />
        </>
      )}

      <Dialog open={atendiendo !== null} onClose={cerrar} maxWidth="sm" fullWidth>
        <DialogTitle>Registrar la llamada</DialogTitle>
        <DialogContent>
          {atendiendo && (
            <>
              <Typography variant="subtitle1">{atendiendo.tutorNombre}</Typography>
              <Typography variant="h6" fontFamily="monospace" color="primary" gutterBottom>
                {atendiendo.destinatario}
              </Typography>

              <Divider sx={{ my: 2 }} />

              <Typography variant="overline" color="text.secondary">
                Lo que hay que comunicarle
              </Typography>
              <Card variant="outlined" sx={{ mb: 2 }}>
                <CardContent sx={{ py: 1.5 }}>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
                    {atendiendo.cuerpo}
                  </Typography>
                </CardContent>
              </Card>

              <TextField
                label="Que le dijiste, o por que no se pudo"
                value={observacion}
                onChange={(e) => setObservacion(e.target.value)}
                fullWidth
                required
                multiline
                rows={3}
                autoFocus
                helperText="Queda en el expediente del trámite, para quien lo revise después"
                inputProps={{ maxLength: 1000 }}
              />
            </>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
          <Button onClick={cerrar} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="outlined"
            color="warning"
            onClick={() => registrar(false)}
            disabled={registrando || !observacion.trim()}
          >
            No contesto
          </Button>
          <Button
            variant="contained"
            color="success"
            onClick={() => registrar(true)}
            disabled={registrando || !observacion.trim()}
          >
            Si le avise
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default BandejaContacto;
