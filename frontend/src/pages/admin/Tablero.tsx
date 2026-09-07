import { Box, Card, CardContent, Grid, Typography } from '@mui/material';

import { useAuth } from '@/context/AuthContext';
import { guinda } from '@/theme';

/**
 * Pantalla de inicio del panel.
 *
 * <p>Los indicadores llegan en la siguiente entrega, cuando haya tramites que
 * contar. Por ahora recibe a la persona y le dice donde esta.
 */
export function Tablero() {
  const { sesion } = useAuth();

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Bienvenida, {sesion?.nombreCompleto}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Padrón Estatal de Perros y Gatos
      </Typography>

      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} color={guinda[700]}>
                Trámites
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Revisa y resuelve las solicitudes que llegan del portal, de
                ventanilla y de las jornadas municipales.
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} color={guinda[700]}>
                Catálogos
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Ajusta los valores que alimentan los formularios sin depender de
                una actualizacion del sistema.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default Tablero;
