import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import BadgeIcon from '@mui/icons-material/Badge';
import DescriptionIcon from '@mui/icons-material/Description';
import DownloadIcon from '@mui/icons-material/Download';
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import EstatusChip from '@/components/EstatusChip';
import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { catalogos } from '@/services/catalogoService';
import certificadoService from '@/services/certificadoService';
import documentoService from '@/services/documentoService';
import solicitudService from '@/services/solicitudService';
import type {
  Catalogo,
  Certificado,
  Documento,
  MovimientoHistorial,
  Solicitud,
} from '@/types/dominio';

/** Resolucion que el personal puede tomar. */
type Resolucion = 'aprobar' | 'observar' | 'rechazar' | 'cancelar' | 'entregar';

const RESOLUCIONES: Record<Resolucion, { titulo: string; exigeMotivo: boolean; color: 'success' | 'warning' | 'error' | 'inherit' }> = {
  aprobar: { titulo: 'Aprobar tramite', exigeMotivo: false, color: 'success' },
  observar: { titulo: 'Devolver para correccion', exigeMotivo: true, color: 'warning' },
  rechazar: { titulo: 'Rechazar tramite', exigeMotivo: true, color: 'error' },
  cancelar: { titulo: 'Cancelar tramite', exigeMotivo: true, color: 'inherit' },
  entregar: { titulo: 'Registrar entrega', exigeMotivo: false, color: 'success' },
};

/** Recorrido del tramite, para ubicar en que punto va. */
const RECORRIDO = ['REGISTRADA', 'EN_REVISION', 'APROBADA', 'CERTIFICADO_EMITIDO', 'ENTREGADA'];

/**
 * Detalle de un tramite.
 *
 * <p>Se presenta a dos columnas: a la izquierda lo que la persona capturo, a la
 * derecha lo que adjunto. El personal coteja una cosa contra la otra sin
 * cambiar de pantalla, que es exactamente lo que hace al revisar.
 */
export function DetalleSolicitud() {
  const { id } = useParams<{ id: string }>();
  const solicitudId = Number(id);

  const [solicitud, setSolicitud] = useState<Solicitud | null>(null);
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [historial, setHistorial] = useState<MovimientoHistorial[]>([]);
  const [certificado, setCertificado] = useState<Certificado | null>(null);
  const [motivos, setMotivos] = useState<Catalogo[]>([]);
  const [cargando, setCargando] = useState(true);

  const [resolucion, setResolucion] = useState<Resolucion | null>(null);
  const [motivoId, setMotivoId] = useState<number | ''>('');
  const [observacion, setObservacion] = useState('');
  const [resolviendo, setResolviendo] = useState(false);

  const [bajaAbierta, setBajaAbierta] = useState(false);
  const [motivoBaja, setMotivoBaja] = useState('');
  const [incluirPersona, setIncluirPersona] = useState(false);
  const [dandoDeBaja, setDandoDeBaja] = useState(false);

  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [detalle, adjuntos, movimientos] = await Promise.all([
        solicitudService.obtener(solicitudId),
        documentoService.listarPorSolicitud(solicitudId),
        solicitudService.historial(solicitudId),
      ]);
      setSolicitud(detalle);
      setDocumentos(adjuntos);
      setHistorial(movimientos);

      // El certificado solo existe si el tramite ya se aprobo.
      try {
        setCertificado(await certificadoService.porSolicitud(solicitudId));
      } catch {
        setCertificado(null);
      }
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [solicitudId]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  useEffect(() => {
    catalogos.motivosRechazo.opciones().then(setMotivos).catch(() => setMotivos([]));
  }, []);

  async function tomarParaRevision() {
    try {
      await solicitudService.tomarParaRevision(solicitudId);
      notificacion.exito(
        'El tramite quedo en revision. Ya puedes aprobarlo, devolverlo para ' +
          'correccion o rechazarlo.',
      );
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  async function resolver() {
    if (!resolucion) return;

    setResolviendo(true);
    try {
      const peticion = {
        motivoId: motivoId === '' ? undefined : motivoId,
        observacion: observacion.trim() || undefined,
      };

      const acciones = {
        aprobar: () => solicitudService.aprobar(solicitudId, peticion),
        observar: () => solicitudService.observar(solicitudId, peticion),
        rechazar: () => solicitudService.rechazar(solicitudId, peticion),
        cancelar: () => solicitudService.cancelar(solicitudId, peticion),
        entregar: () => solicitudService.registrarEntrega(solicitudId, peticion),
      };

      await acciones[resolucion]();

      const avisos: Record<Resolucion, string> = {
        aprobar:
          'Tramite aprobado. El certificado quedo emitido y ya se puede ' +
          'imprimir junto con la credencial.',
        observar:
          'Se devolvio para correccion. A la persona se le avisa el motivo ' +
          'para que corrija y vuelva a enviarlo.',
        rechazar:
          'Tramite rechazado. A la persona se le avisa el motivo; el tramite ' +
          'ya no admite mas cambios.',
        cancelar: 'Tramite cancelado. Ya no admite mas cambios.',
        entregar:
          'Se registro la entrega. El tramite queda concluido: la persona ya ' +
          'tiene su certificado y su credencial.',
      };
      notificacion.exito(avisos[resolucion]);

      cerrarResolucion();
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setResolviendo(false);
    }
  }

  function cerrarResolucion() {
    setResolucion(null);
    setMotivoId('');
    setObservacion('');
  }

  async function darDeBaja() {
    setDandoDeBaja(true);
    try {
      const mensaje = await solicitudService.darDeBaja(
        solicitudId,
        motivoBaja,
        incluirPersona,
      );
      notificacion.exito(mensaje);
      navegar('/admin/solicitudes');
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setDandoDeBaja(false);
    }
  }

  async function descargar(documento: Documento) {
    try {
      await documentoService.descargar(documento.id, documento.nombreOriginal);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  async function marcarLegibilidad(documento: Documento, legible: boolean) {
    try {
      await documentoService.revisar(documento.id, legible);
      notificacion.exito(
        legible
          ? `"${documento.tipoNombre}" quedo marcado como legible`
          : `"${documento.tipoNombre}" quedo marcado como ilegible. ` +
              'Devuelve el tramite para correccion indicando ese motivo.',
      );
      await cargar();
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando tramite" />
      </Box>
    );
  }

  if (!solicitud) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">No se encontro el tramite</Typography>
        <Button onClick={() => navegar('/admin/solicitudes')} sx={{ mt: 2 }}>
          Volver a la bandeja
        </Button>
      </Box>
    );
  }

  const puedeRevisar = tienePermiso('solicitud:editar');
  const enRevision = solicitud.estatusClave === 'EN_REVISION';
  const registrada = solicitud.estatusClave === 'REGISTRADA';
  const pasoActual = RECORRIDO.indexOf(solicitud.estatusClave);

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navegar('/admin/solicitudes')} aria-label="Volver">
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" fontWeight={600}>
            {solicitud.folio}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Recibido el {new Date(solicitud.fechaRegistro).toLocaleString('es-MX')} por{' '}
            {solicitud.canal.toLowerCase()}
          </Typography>
        </Box>
        <EstatusChip
          clave={solicitud.estatusClave}
          nombre={solicitud.estatusNombre}
          color={solicitud.estatusColor}
          size="medium"
        />
      </Stack>

      {solicitud.revisionReforzada && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <Typography variant="subtitle2">Este tramite requiere revision reforzada</Typography>
          <Typography variant="body2">{solicitud.motivoRevisionReforzada}</Typography>
        </Alert>
      )}

      {pasoActual >= 0 && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Stepper activeStep={pasoActual} alternativeLabel>
              {['Registrada', 'En revision', 'Aprobada', 'Certificado', 'Entregada'].map(
                (paso) => (
                  <Step key={paso}>
                    <StepLabel>{paso}</StepLabel>
                  </Step>
                ),
              )}
            </Stepper>
          </CardContent>
        </Card>
      )}

      {/* Dos columnas: lo capturado frente a lo adjuntado, para cotejar. */}
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Datos capturados
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Typography variant="overline" color="text.secondary">
                Tutor responsable
              </Typography>
              <Dato etiqueta="Nombre" valor={solicitud.tutorNombre} />
              <Dato etiqueta="CURP" valor={solicitud.tutorCurpEnmascarada} monoespaciado />
              <Dato etiqueta="Municipio" valor={solicitud.tutorMunicipio} />

              <Typography variant="overline" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                Animal
              </Typography>
              <Dato etiqueta="Nombre" valor={solicitud.animalNombre} />
              <Dato etiqueta="Especie" valor={solicitud.animalEspecie} />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Documentos adjuntos
              </Typography>
              <Divider sx={{ mb: 2 }} />

              {documentos.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No se adjunto ningun documento
                </Typography>
              ) : (
                <Stack spacing={1}>
                  {documentos.map((documento) => (
                    <Box
                      key={documento.id}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        p: 1,
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: 1,
                      }}
                    >
                      <DescriptionIcon color="action" fontSize="small" />
                      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Typography variant="body2" noWrap>
                          {documento.tipoNombre}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" noWrap>
                          {documento.nombreOriginal}
                        </Typography>
                      </Box>

                      {documento.legible === false && (
                        <Chip label="Ilegible" size="small" color="error" />
                      )}
                      {documento.legible === true && (
                        <Chip label="Legible" size="small" color="success" variant="outlined" />
                      )}

                      {tienePermiso('documento:descargar') && (
                        <Tooltip title="Descargar">
                          <IconButton
                            size="small"
                            onClick={() => descargar(documento)}
                            aria-label={`Descargar ${documento.tipoNombre}`}
                          >
                            <DownloadIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}

                      {tienePermiso('documento:revisar') && documento.legible !== false && (
                        <Tooltip title="Marcar ilegible">
                          <Button
                            size="small"
                            color="warning"
                            onClick={() => marcarLegibilidad(documento, false)}
                          >
                            Ilegible
                          </Button>
                        </Tooltip>
                      )}
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {certificado && (
        <Card sx={{ mt: 2 }}>
          <CardContent>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="subtitle1" fontWeight={600}>
                  Certificado emitido
                </Typography>
                <Typography variant="body2" fontFamily="monospace">
                  {certificado.folio}
                </Typography>
                {!certificado.vigente && (
                  <Chip label="Cancelado" size="small" color="error" sx={{ mt: 0.5 }} />
                )}
              </Box>

              {tienePermiso('certificado:imprimir') && (
                <Stack direction="row" spacing={1}>
                  <Button
                    startIcon={<DescriptionIcon />}
                    variant="outlined"
                    onClick={() =>
                      certificadoService
                        .descargarCertificado(certificado.id, certificado.folio)
                        .catch((fallo) => notificacion.error(mensajeDeError(fallo)))
                    }
                  >
                    Certificado
                  </Button>
                  <Button
                    startIcon={<BadgeIcon />}
                    variant="outlined"
                    onClick={() =>
                      certificadoService
                        .descargarCredencial(certificado.id, certificado.folio)
                        .catch((fallo) => notificacion.error(mensajeDeError(fallo)))
                    }
                  >
                    Credencial
                  </Button>
                </Stack>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}

      {solicitud.estatusClave === 'CERTIFICADO_EMITIDO' &&
        tienePermiso('certificado:emitir') && (
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Entrega
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Cuando la persona recoja su certificado y su credencial, registra
                la entrega para cerrar el tramite.
              </Typography>
              <Button
                variant="contained"
                color="success"
                onClick={() => setResolucion('entregar')}
              >
                Registrar entrega
              </Button>
            </CardContent>
          </Card>
        )}

      {puedeRevisar && (registrada || enRevision) && (
        <Card sx={{ mt: 2 }}>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Resolucion
            </Typography>

            {registrada ? (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  Toma el tramite para revisarlo antes de resolverlo.
                </Typography>
                <Button variant="contained" onClick={tomarParaRevision}>
                  Tomar para revision
                </Button>
              </>
            ) : (
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                {tienePermiso('solicitud:aprobar') && (
                  <Button
                    variant="contained"
                    color="success"
                    onClick={() => setResolucion('aprobar')}
                  >
                    Aprobar
                  </Button>
                )}
                {tienePermiso('solicitud:observar') && (
                  <Button
                    variant="outlined"
                    color="warning"
                    onClick={() => setResolucion('observar')}
                  >
                    Devolver para correccion
                  </Button>
                )}
                {tienePermiso('solicitud:rechazar') && (
                  <Button
                    variant="outlined"
                    color="error"
                    onClick={() => setResolucion('rechazar')}
                  >
                    Rechazar
                  </Button>
                )}
              </Stack>
            )}
          </CardContent>
        </Card>
      )}

      {tienePermiso('solicitud:depurar') &&
        solicitud.estatusClave !== 'CERTIFICADO_EMITIDO' &&
        solicitud.estatusClave !== 'ENTREGADA' && (
          <Card sx={{ mt: 2, borderColor: 'error.light' }}>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Depurar del padron
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Usa esto solo si el registro nunca fue real: alguien probando el
                sistema, o datos evidentemente falsos. Si es una persona que
                simplemente no concluyo su tramite, cancelalo en lugar de darlo
                de baja.
              </Typography>
              <Button
                variant="outlined"
                color="error"
                onClick={() => setBajaAbierta(true)}
              >
                Dar de baja
              </Button>
            </CardContent>
          </Card>
        )}

      <Card sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="subtitle1" fontWeight={600} gutterBottom>
            Expediente
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Stack spacing={1.5}>
            {historial.map((movimiento) => (
              <Box key={movimiento.id} sx={{ display: 'flex', gap: 2 }}>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ minWidth: 130, flexShrink: 0 }}
                >
                  {new Date(movimiento.fecha).toLocaleString('es-MX')}
                </Typography>
                <Box>
                  <Typography variant="body2">
                    {movimiento.estatusAnterior
                      ? `${movimiento.estatusAnterior} a ${movimiento.estatusNuevo}`
                      : `Alta: ${movimiento.estatusNuevo}`}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {movimiento.usuario}
                    {movimiento.motivo ? ` - ${movimiento.motivo}` : ''}
                  </Typography>
                  {movimiento.observacion && (
                    <Typography variant="caption" display="block" color="text.secondary">
                      {movimiento.observacion}
                    </Typography>
                  )}
                </Box>
              </Box>
            ))}
          </Stack>
        </CardContent>
      </Card>

      <Dialog
        open={bajaAbierta}
        onClose={() => setBajaAbierta(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Dar de baja del padron</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            El registro deja de aparecer en el padron y en las busquedas, pero no
            se borra: queda con tu nombre y el motivo por si despues hay que
            explicarlo. Se puede reactivar.
          </Alert>

          <TextField
            label="Por que se da de baja"
            value={motivoBaja}
            onChange={(e) => setMotivoBaja(e.target.value)}
            fullWidth
            required
            multiline
            rows={2}
            autoFocus
            helperText="Por ejemplo: datos de prueba, registro duplicado, informacion falsa"
            inputProps={{ maxLength: 500 }}
          />

          <FormControlLabel
            sx={{ mt: 2 }}
            control={
              <Checkbox
                checked={incluirPersona}
                onChange={(e) => setIncluirPersona(e.target.checked)}
              />
            }
            label="Dar de baja tambien al tutor y su animal"
          />

          <Typography variant="caption" color="text.secondary" display="block">
            {incluirPersona
              ? 'El tutor solo se da de baja si este era su unico tramite vigente. ' +
                'Marcalo cuando el registro completo sea basura.'
              : 'Solo se da de baja el tramite. El tutor y su animal siguen en el ' +
                'padron, por si la persona vuelve a registrarse.'}
          </Typography>
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setBajaAbierta(false)} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={darDeBaja}
            disabled={dandoDeBaja || !motivoBaja.trim()}
          >
            {dandoDeBaja ? 'Dando de baja...' : 'Dar de baja'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={resolucion !== null} onClose={cerrarResolucion} maxWidth="sm" fullWidth>
        <DialogTitle>{resolucion ? RESOLUCIONES[resolucion].titulo : ''}</DialogTitle>
        <DialogContent>
          {resolucion === 'aprobar' && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Al aprobar se emite el certificado y queda listo para imprimir.
            </Alert>
          )}

          {resolucion === 'entregar' && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Confirma que la persona recibio su certificado y su credencial. El
              tramite queda concluido y no admitira mas cambios.
            </Alert>
          )}

          {resolucion && RESOLUCIONES[resolucion].exigeMotivo && (
            <FormControl fullWidth margin="normal" required>
              <InputLabel id="etiqueta-motivo">Motivo</InputLabel>
              <Select
                labelId="etiqueta-motivo"
                label="Motivo"
                value={motivoId}
                onChange={(e) => setMotivoId(Number(e.target.value))}
              >
                {motivos.map((motivo) => (
                  <MenuItem key={motivo.id} value={motivo.id}>
                    {motivo.nombre}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          <TextField
            label="Observacion"
            value={observacion}
            onChange={(e) => setObservacion(e.target.value)}
            fullWidth
            multiline
            rows={3}
            margin="normal"
            helperText="Este texto se le hace llegar a la persona solicitante"
            inputProps={{ maxLength: 1000 }}
          />
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={cerrarResolucion} color="inherit">
            Cancelar
          </Button>
          <Button
            variant="contained"
            color={resolucion ? RESOLUCIONES[resolucion].color : 'primary'}
            onClick={resolver}
            disabled={
              resolviendo ||
              (resolucion !== null && RESOLUCIONES[resolucion].exigeMotivo && motivoId === '')
            }
          >
            {resolviendo ? 'Guardando...' : 'Confirmar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

/** Etiqueta con su valor, para las fichas de datos. */
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
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 100 }}>
        {etiqueta}
      </Typography>
      <Typography variant="body2" fontFamily={monoespaciado ? 'monospace' : undefined}>
        {valor ?? '-'}
      </Typography>
    </Box>
  );
}

export default DetalleSolicitud;
