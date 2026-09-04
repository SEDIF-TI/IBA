import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';

import { mensajeDeError } from '@/services/api';
import certificadoService from '@/services/certificadoService';
import { estado, guinda } from '@/theme';
import type { Verificacion } from '@/types/dominio';

/**
 * Verificacion publica de un certificado.
 *
 * <p>Es la pantalla a la que lleva el codigo QR impreso en el documento. Quien
 * llega aqui puede ser cualquiera: un veterinario, un vecino, quien encontro al
 * animal perdido. Por eso confirma que el certificado es autentico y describe
 * al animal, pero no muestra nada del tutor.
 *
 * <p>Tambien admite capturar el codigo a mano, para quien tiene el documento en
 * papel y no puede escanear.
 */
export function VerificarCertificado() {
  const { codigo: codigoRuta } = useParams<{ codigo?: string }>();

  const [codigo, setCodigo] = useState(codigoRuta ?? '');
  const [resultado, setResultado] = useState<Verificacion | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [verificando, setVerificando] = useState(false);

  const verificar = useCallback(async (valor: string) => {
    setError(null);
    setResultado(null);
    setVerificando(true);

    try {
      setResultado(await certificadoService.verificar(valor.trim()));
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setVerificando(false);
    }
  }, []);

  // Si el codigo viene en la direccion, se verifica solo: quien escaneo el QR
  // no deberia tener que pulsar nada mas.
  useEffect(() => {
    if (codigoRuta) {
      void verificar(codigoRuta);
    }
  }, [codigoRuta, verificar]);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    void verificar(codigo);
  }

  return (
    <Box sx={{ maxWidth: 620, mx: 'auto' }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Verificar certificado
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Comprueba que un certificado de empadronamiento fue expedido por el
        Instituto de Bienestar Animal.
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box component="form" onSubmit={enviar} noValidate>
            <TextField
              label="Código de verificación"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value)}
              fullWidth
              required
              margin="normal"
              placeholder="550e8400-e29b-41d4-a716-446655440000"
              helperText="Lo encuentras al pie del documento, junto al código QR"
              disabled={verificando}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              sx={{ mt: 2 }}
              disabled={verificando || !codigo.trim()}
            >
              {verificando ? 'Verificando...' : 'Verificar'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {verificando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress aria-label="Verificando el certificado" />
        </Box>
      )}

      {error && (
        <Alert severity="error" role="alert">
          {error}
        </Alert>
      )}

      {resultado && !verificando && (
        <Card>
          <CardContent sx={{ p: 3 }}>
            {/* El icono acompana al texto, no lo sustituye: quien no distingue
                el color tiene que poder leer el resultado igual. */}
            <Stack direction="row" spacing={1.5} alignItems="flex-start" sx={{ mb: 2 }}>
              {resultado.valido ? (
                <CheckCircleIcon sx={{ color: estado.exito.main, fontSize: 34 }} aria-hidden />
              ) : (
                <ErrorIcon sx={{ color: estado.error.main, fontSize: 34 }} aria-hidden />
              )}
              <Box>
                <Typography
                  variant="h6"
                  sx={{ color: resultado.valido ? estado.exito.dark : estado.error.dark }}
                >
                  {resultado.valido ? 'Certificado autentico' : 'Certificado no vigente'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {resultado.mensaje}
                </Typography>
              </Box>
            </Stack>

            {resultado.folio && (
              <>
                <Divider sx={{ my: 2 }} />

                <Typography variant="overline" color="text.secondary">
                  Folio del certificado
                </Typography>
                <Typography variant="body1" fontFamily="monospace" sx={{ mb: 2 }}>
                  {resultado.folio}
                </Typography>

                <Typography variant="overline" color="text.secondary">
                  Ser sintiente registrado
                </Typography>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="h6" sx={{ color: guinda[700] }}>
                    {resultado.animalNombre}
                  </Typography>
                  <Stack direction="row" spacing={2} flexWrap="wrap">
                    <Detalle etiqueta="Especie" valor={resultado.animalEspecie} />
                    <Detalle etiqueta="Raza" valor={resultado.animalRaza} />
                    <Detalle etiqueta="Sexo" valor={resultado.animalSexo} />
                    <Detalle etiqueta="Color" valor={resultado.animalColor} />
                    <Detalle etiqueta="Edad" valor={resultado.animalEdad} />
                  </Stack>
                </Box>

                <Stack direction="row" spacing={3} flexWrap="wrap">
                  <Detalle etiqueta="Municipio" valor={resultado.municipio} />
                  <Detalle
                    etiqueta="Emitido"
                    valor={
                      resultado.fechaEmision
                        ? new Date(resultado.fechaEmision).toLocaleDateString('es-MX')
                        : undefined
                    }
                  />
                  <Detalle
                    etiqueta="Vigencia"
                    valor={
                      resultado.fechaVigencia
                        ? new Date(resultado.fechaVigencia).toLocaleDateString('es-MX')
                        : 'Sin vencimiento'
                    }
                  />
                </Stack>

                <Alert severity="info" sx={{ mt: 3 }}>
                  Por proteccion de datos personales, esta consulta no muestra la
                  informacion del tutor responsable.
                </Alert>
              </>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
}

/** Etiqueta con su valor; se omite si no hay dato. */
function Detalle({ etiqueta, valor }: { etiqueta: string; valor?: string | null }) {
  if (!valor) {
    return null;
  }
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" display="block">
        {etiqueta}
      </Typography>
      <Typography variant="body2">{valor}</Typography>
    </Box>
  );
}

export default VerificarCertificado;
