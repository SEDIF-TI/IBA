import { Box, Button, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

/** Pantalla para cuando la persona no tiene el permiso necesario. */
export function SinPermiso() {
  return (
    <Box sx={{ textAlign: 'center', py: 8 }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        No tienes acceso a esta seccion
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Si necesitas entrar aquí, pidele a informatica que revise tus permisos.
      </Typography>
      <Button component={RouterLink} to="/admin" variant="contained">
        Volver al inicio
      </Button>
    </Box>
  );
}

export default SinPermiso;
