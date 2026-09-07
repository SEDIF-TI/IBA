import { useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';

import type { Catalogo } from '@/types/dominio';

/** Lo que el servidor acepta. Se repite aqui para avisar antes de subir. */
const MIMES = ['application/pdf', 'image/jpeg', 'image/png'];
const MAXIMO_BYTES = 10 * 1024 * 1024;

export interface Props {
  /** Tipos de documento que se pueden elegir. */
  tipos: Catalogo[];
  /** Sube el archivo. Lo resuelve quien usa el componente. */
  alSubir: (tipoDocumentoId: number, archivo: File) => Promise<void>;
  /** Texto de ayuda propio de la pantalla. */
  ayuda?: string;
  deshabilitado?: boolean;
}

/**
 * Carga de un documento: elegir tipo, elegir archivo, subir.
 *
 * <p>Comprueba formato y tamano antes de enviar. No es la barrera real (esa
 * esta en el servidor, que revisa el contenido y no la extension), pero evita
 * que la persona espere la subida de un archivo que se va a rechazar.
 */
export function CargaDocumento({ tipos, alSubir, ayuda, deshabilitado = false }: Props) {
  const [tipoId, setTipoId] = useState<number>(0);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [subiendo, setSubiendo] = useState(false);
  const campoArchivo = useRef<HTMLInputElement>(null);

  function elegir(elegido: File | null) {
    setError('');
    if (!elegido) {
      setArchivo(null);
      return;
    }

    if (!MIMES.includes(elegido.type)) {
      setError(
        `"${elegido.name}" no es un formato aceptado. ` +
          'Solo se reciben archivos PDF, JPG o PNG.',
      );
      setArchivo(null);
      return;
    }

    if (elegido.size > MAXIMO_BYTES) {
      const mb = (elegido.size / 1024 / 1024).toFixed(1);
      setError(
        `"${elegido.name}" pesa ${mb} MB y el maximo son 10 MB. ` +
          'Si es una foto, vuelve a tomarla con menor calidad.',
      );
      setArchivo(null);
      return;
    }

    setArchivo(elegido);
  }

  async function subir() {
    if (!tipoId) {
      setError('Elige primero que documento vas a subir.');
      return;
    }
    if (!archivo) {
      setError('Elige el archivo que quieres subir.');
      return;
    }

    setSubiendo(true);
    try {
      await alSubir(tipoId, archivo);
      // Queda listo para el siguiente: casi nunca se sube uno solo.
      setArchivo(null);
      setTipoId(0);
      if (campoArchivo.current) campoArchivo.current.value = '';
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <Box>
      {ayuda && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {ayuda}
        </Typography>
      )}

      <Stack spacing={2}>
        <TextField
          select
          label="¿Qué documento es?"
          value={tipoId || ''}
          onChange={(e) => {
            setTipoId(Number(e.target.value));
            setError('');
          }}
          disabled={deshabilitado || subiendo}
          fullWidth
          size="small"
        >
          {tipos.map((tipo) => (
            <MenuItem key={tipo.id} value={tipo.id}>
              {tipo.nombre}
            </MenuItem>
          ))}
        </TextField>

        <Button
          component="label"
          variant="outlined"
          startIcon={<InsertDriveFileIcon />}
          disabled={deshabilitado || subiendo}
          sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
        >
          {archivo ? archivo.name : 'Elegir archivo (PDF, JPG o PNG, máximo 10 MB)'}
          <input
            ref={campoArchivo}
            type="file"
            hidden
            accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
            onChange={(e) => elegir(e.target.files?.[0] ?? null)}
          />
        </Button>

        {error && <Alert severity="error">{error}</Alert>}

        <Box>
          <Button
            variant="contained"
            startIcon={subiendo ? <CircularProgress size={18} color="inherit" /> : <CloudUploadIcon />}
            onClick={() => void subir()}
            disabled={deshabilitado || subiendo || !archivo || !tipoId}
          >
            {subiendo ? 'Subiendo...' : 'Subir documento'}
          </Button>
        </Box>
      </Stack>
    </Box>
  );
}

export default CargaDocumento;
