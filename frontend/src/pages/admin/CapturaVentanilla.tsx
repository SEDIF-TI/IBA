import { Alert, Box } from '@mui/material';

import RegistroTramite from '@/pages/publico/RegistroTramite';

/**
 * Captura de un tramite en ventanilla.
 *
 * <p>Reutiliza el formulario del portal: es el mismo tramite, capturado por
 * otra persona. Mantener dos formularios distintos garantizaria que se
 * separaran con el tiempo y que una correccion se aplicara solo en uno.
 */
export function CapturaVentanilla() {
  return (
    <Box>
      <Alert severity="info" sx={{ mb: 2 }}>
        Usa esta pantalla cuando la persona llega sin haberse registrado. El
        tramite queda marcado como capturado en ventanilla.
      </Alert>

      <RegistroTramite ventanilla />
    </Box>
  );
}

export default CapturaVentanilla;
