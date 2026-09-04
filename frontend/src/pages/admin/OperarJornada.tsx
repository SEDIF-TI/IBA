import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Grid,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import BadgeIcon from '@mui/icons-material/Badge';
import ClearIcon from '@mui/icons-material/Clear';
import PrintIcon from '@mui/icons-material/Print';
import SearchIcon from '@mui/icons-material/Search';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import EstatusChip from '@/components/EstatusChip';
import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import certificadoService from '@/services/certificadoService';
import jornadaService from '@/services/jornadaService';
import solicitudService from '@/services/solicitudService';
import { guinda, neutro } from '@/theme';
import type { Jornada, ResumenJornada, Solicitud } from '@/types/dominio';

/**
 * Operacion de una jornada en sitio.
 *
 * <p>Esta pantalla se usa de pie, con gente formada enfrente. Por eso el
 * buscador es lo primero y ocupa el ancho completo, los renglones son altos
 * para poder tocarlos sin fallar, y cada tramite se resuelve desde aqui sin
 * abrir otra pantalla.
 *
 * <p>La busqueda tolera acentos: nadie escribe la enie de "Munoz" con prisa.
 */
export function OperarJornada() {
  const { id } = useParams<{ id: string }>();
  const jornadaId = Number(id);

  const [jornada, setJornada] = useState<Jornada | null>(null);
  const [resumen, setResumen] = useState<ResumenJornada | null>(null);
  const [texto, setTexto] = useState('');
  const [resultados, setResultados] = useState<Solicitud[]>([]);
  const [mensajeBusqueda, setMensajeBusqueda] = useState('');
  const [buscando, setBuscando] = useState(false);
  const [cargando, setCargando] = useState(true);

  const [seleccionados, setSeleccionados] = useState<Set<number>>(new Set());
  const [procesando, setProcesando] = useState<number | null>(null);

  const campoBusqueda = useRef<HTMLInputElement>(null);
  const { tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();

  const cargarJornada = useCallback(async () => {
    try {
      const [detalle, avance] = await Promise.all([
        jornadaService.obtener(jornadaId),
        jornadaService.resumen(jornadaId),
      ]);
      setJornada(detalle);
      setResumen(avance);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setCargando(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jornadaId]);

  useEffect(() => {
    void cargarJornada();
  }, [cargarJornada]);

  const buscar = useCallback(
    async (valor: string) => {
      setBuscando(true);
      try {
        const { resultados: pagina, mensaje } = await jornadaService.buscar(
          jornadaId,
          valor,
          { size: 50 },
        );
        setResultados(pagina.content);
        setMensajeBusqueda(mensaje);
      } catch (fallo) {
        notificacion.error(mensajeDeError(fallo));
      } finally {
        setBuscando(false);
      }
      // eslint-disable-next-line react-hooks/exhaustive-deps
    },
    [jornadaId],
  );

  // Al abrir se muestran todas las precargas: el personal necesita ver de
  // entrada a cuanta gente espera.
  useEffect(() => {
    void buscar('');
  }, [buscar]);

  // Se busca sola mientras se escribe, con una pausa breve para no consultar
  // en cada tecla. Medio segundo es lo que tarda en escribirse un apellido.
  useEffect(() => {
    const temporizador = setTimeout(() => void buscar(texto), 500);
    return () => clearTimeout(temporizador);
  }, [texto, buscar]);

  function alternarSeleccion(solicitudId: number) {
    setSeleccionados((anterior) => {
      const nueva = new Set(anterior);
      if (nueva.has(solicitudId)) {
        nueva.delete(solicitudId);
      } else {
        nueva.add(solicitudId);
      }
      return nueva;
    });
  }

  /** Toma el tramite y lo aprueba en un solo paso. */
  async function validarEnSitio(solicitud: Solicitud) {
    setProcesando(solicitud.id);
    try {
      if (solicitud.estatusClave === 'REGISTRADA') {
        await solicitudService.tomarParaRevision(solicitud.id);
      }
      await solicitudService.aprobar(solicitud.id, {
        observacion: 'Validado en sitio durante la jornada',
      });

      notificacion.exito(
        `Tramite de ${solicitud.tutorNombre} aprobado. Su certificado quedo ` +
          'emitido y la credencial lista para imprimir.',
      );

      await Promise.all([buscar(texto), cargarJornada()]);
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setProcesando(null);
    }
  }

  /** Imprime las credenciales de los tramites marcados. */
  async function imprimirSeleccionadas() {
    const conCertificado = resultados.filter(
      (s) => seleccionados.has(s.id) && s.estatusClave === 'CERTIFICADO_EMITIDO',
    );

    if (conCertificado.length === 0) {
      notificacion.advertencia(
        'Ninguno de los tramites marcados tiene certificado emitido. ' +
          'Valida primero los tramites para poder imprimir sus credenciales.',
      );
      return;
    }

    try {
      // El identificador del certificado se obtiene del tramite.
      const certificados = await Promise.all(
        conCertificado.map((s) => certificadoService.porSolicitud(s.id)),
      );
      const ids = certificados.filter((c) => c !== null).map((c) => c!.id);

      await certificadoService.imprimirLote(ids);

      notificacion.exito(
        `Se prepararon ${ids.length} credenciales para imprimir, ` +
          'tres por hoja. Recortalas por las marcas de las esquinas.',
      );
      setSeleccionados(new Set());
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    }
  }

  if (cargando) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress aria-label="Cargando la jornada" />
      </Box>
    );
  }

  if (!jornada) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">No se encontro la jornada</Typography>
        <Button onClick={() => navegar('/admin/jornadas')} sx={{ mt: 2 }}>
          Volver a jornadas
        </Button>
      </Box>
    );
  }

  const marcadosConCertificado = resultados.filter(
    (s) => seleccionados.has(s.id) && s.estatusClave === 'CERTIFICADO_EMITIDO',
  ).length;

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navegar('/admin/jornadas')} aria-label="Volver">
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flexGrow: 1 }}>
          <Typography variant="h5" fontWeight={600}>
            {jornada.nombre}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {jornada.municipio} - {jornada.domicilioSede}
            {jornada.horario ? ` - ${jornada.horario}` : ''}
          </Typography>
        </Box>
        {jornada.enCurso && <Chip label="En curso" color="success" />}
      </Stack>

      {resumen && (
        <Grid container spacing={2} sx={{ mb: 2 }}>
          <Indicador etiqueta="Precargadas" valor={resumen.precargadas} />
          <Indicador etiqueta="Atendidas" valor={resumen.atendidas} color="success.main" />
          <Indicador etiqueta="Pendientes" valor={resumen.pendientes} color="warning.main" />
          {resumen.lugaresLibres !== null && resumen.lugaresLibres !== undefined && (
            <Indicador etiqueta="Lugares libres" valor={resumen.lugaresLibres} />
          )}
        </Grid>
      )}

      {/* El buscador es lo primero y ocupa el ancho completo: es lo que se usa
          todo el dia, con la gente esperando. */}
      <Card sx={{ mb: 2, borderColor: guinda[200] }}>
        <CardContent>
          <TextField
            inputRef={campoBusqueda}
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            fullWidth
            autoFocus
            placeholder="Busca por CURP, apellidos o nombre"
            helperText="Puedes escribir sin acentos: 'munoz' encuentra 'Muñoz'"
            InputProps={{
              sx: { fontSize: '1.25rem', py: 0.5 },
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="large" color="action" />
                </InputAdornment>
              ),
              endAdornment: texto && (
                <InputAdornment position="end">
                  <IconButton onClick={() => setTexto('')} aria-label="Limpiar busqueda">
                    <ClearIcon />
                  </IconButton>
                </InputAdornment>
              ),
            }}
            inputProps={{ 'aria-label': 'Buscar persona en la jornada' }}
          />
        </CardContent>
      </Card>

      {tienePermiso('certificado:imprimir') && seleccionados.size > 0 && (
        <Alert
          severity="info"
          sx={{ mb: 2 }}
          action={
            <Button
              startIcon={<PrintIcon />}
              variant="contained"
              size="small"
              onClick={imprimirSeleccionadas}
            >
              Imprimir {marcadosConCertificado}
            </Button>
          }
        >
          {seleccionados.size} tramite(s) marcado(s)
          {marcadosConCertificado < seleccionados.size &&
            `, de los cuales ${marcadosConCertificado} tienen certificado emitido`}
        </Alert>
      )}

      {buscando ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress aria-label="Buscando" />
        </Box>
      ) : resultados.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <Typography color="text.secondary">{mensajeBusqueda}</Typography>
          </CardContent>
        </Card>
      ) : (
        <Stack spacing={1}>
          <Typography variant="caption" color="text.secondary">
            {mensajeBusqueda}
          </Typography>

          {resultados.map((solicitud) => {
            const yaValidado =
              solicitud.estatusClave === 'CERTIFICADO_EMITIDO' ||
              solicitud.estatusClave === 'ENTREGADA';
            const sePuedeValidar =
              solicitud.estatusClave === 'REGISTRADA' ||
              solicitud.estatusClave === 'EN_REVISION';

            return (
              <Card
                key={solicitud.id}
                sx={{
                  borderLeft: 4,
                  borderLeftColor: yaValidado ? 'success.main' : neutro[300],
                }}
              >
                {/* Renglones altos: se tocan con el dedo, de pie. */}
                <CardContent sx={{ py: 2 }}>
                  <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    alignItems={{ md: 'center' }}
                  >
                    {tienePermiso('certificado:imprimir') && (
                      <Checkbox
                        checked={seleccionados.has(solicitud.id)}
                        onChange={() => alternarSeleccion(solicitud.id)}
                        inputProps={{
                          'aria-label': `Marcar el tramite de ${solicitud.tutorNombre}`,
                        }}
                        sx={{ p: 1.5 }}
                      />
                    )}

                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                      <Typography variant="h6" sx={{ lineHeight: 1.3 }}>
                        {solicitud.tutorNombre}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {solicitud.animalNombre} - {solicitud.animalEspecie}
                        {solicitud.tutorCurpEnmascarada
                          ? ` - ${solicitud.tutorCurpEnmascarada}`
                          : ''}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" fontFamily="monospace">
                        {solicitud.folio}
                      </Typography>
                    </Box>

                    <EstatusChip
                      clave={solicitud.estatusClave}
                      nombre={solicitud.estatusNombre}
                      color={solicitud.estatusColor}
                      size="medium"
                    />

                    <Stack direction="row" spacing={1}>
                      {sePuedeValidar && tienePermiso('solicitud:aprobar') && (
                        <Button
                          variant="contained"
                          color="success"
                          size="large"
                          disabled={procesando === solicitud.id}
                          onClick={() => validarEnSitio(solicitud)}
                        >
                          {procesando === solicitud.id ? 'Validando...' : 'Validar'}
                        </Button>
                      )}

                      {yaValidado && tienePermiso('certificado:imprimir') && (
                        <Tooltip title="Imprimir credencial">
                          <Button
                            variant="outlined"
                            size="large"
                            startIcon={<BadgeIcon />}
                            onClick={async () => {
                              try {
                                const certificado =
                                  await certificadoService.porSolicitud(solicitud.id);
                                if (certificado) {
                                  await certificadoService.descargarCredencial(
                                    certificado.id,
                                    certificado.folio,
                                  );
                                }
                              } catch (fallo) {
                                notificacion.error(mensajeDeError(fallo));
                              }
                            }}
                          >
                            Credencial
                          </Button>
                        </Tooltip>
                      )}

                      <Button
                        size="large"
                        onClick={() => navegar(`/admin/solicitudes/${solicitud.id}`)}
                      >
                        Detalle
                      </Button>
                    </Stack>
                  </Stack>
                </CardContent>
              </Card>
            );
          })}
        </Stack>
      )}
    </Box>
  );
}

/** Indicador de avance de la jornada. */
function Indicador({
  etiqueta,
  valor,
  color,
}: {
  etiqueta: string;
  valor: number;
  color?: string;
}) {
  return (
    <Grid item xs={6} sm={3}>
      <Card>
        <CardContent sx={{ py: 1.5, textAlign: 'center' }}>
          <Typography variant="h4" fontWeight={600} sx={{ color }}>
            {valor}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {etiqueta}
          </Typography>
        </CardContent>
      </Card>
    </Grid>
  );
}

export default OperarJornada;
