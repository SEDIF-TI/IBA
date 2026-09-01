import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState, type FormEvent } from 'react';

import EstatusChip from '@/components/EstatusChip';
import { mensajeDeError } from '@/services/api';
import solicitudService from '@/services/solicitudService';
import type { Seguimiento } from '@/types/dominio';

/**
 * Consulta del avance del tramite.
 *
 * <p>Pide folio Y CURP. Con la CURP sola, quien conociera la de un tercero
 * podria ver sus datos: no hay cuenta que lo impida, asi que el folio funciona
 * como la prueba de que el tramite es suyo.
 */
export function ConsultaTramite() {
  const [folio, setFolio] = useState('');
  const [curp, setCurp] = useState('');
  const [resultado, setResultado] = useState<Seguimiento | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [consultando, setConsultando] = useState(false);

  async function consultar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    setResultado(null);
    setConsultando(true);

    try {
      setResultado(
        await solicitudService.consultarPublico(folio.trim(), curp.trim().toUpperCase()),
      );
    } catch (fallo) {
      setError(mensajeDeError(fallo));
    } finally {
      setConsultando(false);
    }
  }

  return (
    <Box sx={{ maxWidth: 640, mx: 'auto' }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Consultar mi tramite
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Necesitas el folio que recibiste al registrar y la CURP con la que se
        hizo el tramite.
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box component="form" onSubmit={consultar} noValidate>
            <TextField
              label="Folio de seguimiento"
              value={folio}
              onChange={(e) => setFolio(e.target.value)}
              fullWidth
              required
              autoFocus
              margin="normal"
              placeholder="SOL-20260101-00001"
              disabled={consultando}
            />

            <TextField
              label="CURP"
              value={curp}
              onChange={(e) => setCurp(e.target.value.toUpperCase())}
              fullWidth
              required
              margin="normal"
              inputProps={{ maxLength: 18 }}
              disabled={consultando}
            />

            <Button
              type="submit"
              variant="contained"
              size="large"
              sx={{ mt: 2 }}
              disabled={consultando || !folio.trim() || !curp.trim()}
            >
              {consultando ? 'Consultando...' : 'Consultar'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {error && (
        <Alert severity="error" role="alert" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {resultado && (
        <Card>
          <CardContent sx={{ p: 3 }}>
            <Stack
              direction="row"
              justifyContent="space-between"
              alignItems="center"
              sx={{ mb: 2 }}
            >
              <Typography variant="h6">{resultado.folio}</Typography>
              <EstatusChip
                clave={resultado.estatusClave}
                nombre={resultado.estatusNombre}
                color={resultado.estatusColor}
                size="medium"
              />
            </Stack>

            <Divider sx={{ mb: 2 }} />

            <Typography variant="body2" color="text.secondary">
              Animal registrado
            </Typography>
            <Typography variant="body1" sx={{ mb: 2 }}>
              {resultado.animalNombre}
            </Typography>

            {resultado.estatusDescripcion && (
              <Typography variant="body2" sx={{ mb: 2 }}>
                {resultado.estatusDescripcion}
              </Typography>
            )}

            {resultado.requiereCorreccion && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Tu tramite necesita una correccion
                </Typography>
                {resultado.motivo && <Typography variant="body2">{resultado.motivo}</Typography>}
                {resultado.observacion && (
                  <Typography variant="body2">{resultado.observacion}</Typography>
                )}
              </Alert>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
}

export default ConsultaTramite;
