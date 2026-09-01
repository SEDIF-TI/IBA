import { Box, Button, Card, CardContent, Grid, Stack, Typography } from '@mui/material';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import SearchIcon from '@mui/icons-material/Search';
import { Link as RouterLink } from 'react-router-dom';

import { guinda } from '@/theme';

/**
 * Portada del portal ciudadano.
 *
 * <p>Dos caminos y nada mas: registrar o consultar. No hay que crear cuenta ni
 * esperar a que alguien la valide, que es donde se perdia la gente en el
 * sistema anterior.
 */
export function Inicio() {
  return (
    <Box>
      <Box sx={{ textAlign: 'center', py: { xs: 3, md: 5 } }}>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          Empadrona a tu perro o gato
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 620, mx: 'auto' }}>
          Registra a tu compañero en el Padron Estatal y obten su certificado de
          empadronamiento y su credencial. No necesitas crear una cuenta.
        </Typography>
      </Box>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <AssignmentTurnedInIcon sx={{ fontSize: 40, color: guinda[500] }} aria-hidden />
              <Typography variant="h6" sx={{ mt: 1 }} gutterBottom>
                Registrar
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Llena el formulario con tus datos, los de tu animal y adjunta tu
                documentacion. Al terminar recibes un folio de seguimiento.
              </Typography>
              <Button component={RouterLink} to="/registro" variant="contained" size="large">
                Iniciar registro
              </Button>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ p: 3 }}>
              <SearchIcon sx={{ fontSize: 40, color: guinda[500] }} aria-hidden />
              <Typography variant="h6" sx={{ mt: 1 }} gutterBottom>
                Consultar
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Revisa en que va tu tramite con el folio que recibiste y tu CURP.
              </Typography>
              <Button component={RouterLink} to="/consulta" variant="outlined" size="large">
                Consultar tramite
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <CardContent sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Que necesitas
          </Typography>
          <Stack component="ul" spacing={0.5} sx={{ pl: 2.5, m: 0 }}>
            <Typography component="li" variant="body2">
              Identificacion oficial vigente, legible y por ambos lados
            </Typography>
            <Typography component="li" variant="body2">
              Comprobante de domicilio con no mas de tres meses
            </Typography>
            <Typography component="li" variant="body2">
              Fotografia clara de tu animal, de cuerpo completo
            </Typography>
            <Typography component="li" variant="body2">
              Tu CURP, si la tienes a la mano. Si no, puedes continuar sin ella.
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}

export default Inicio;
