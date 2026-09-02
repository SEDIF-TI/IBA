import { AppBar, Box, Button, Container, Link, Toolbar, Typography } from '@mui/material';
import PetsIcon from '@mui/icons-material/Pets';
import { Link as RouterLink, Outlet } from 'react-router-dom';

import { guinda, neutro } from '@/theme';

/**
 * Estructura del portal ciudadano.
 *
 * <p>Sin sesion ni menu de usuario: aqui no hay cuentas. La ciudadania entra,
 * hace su tramite y se va con su folio.
 */
export function PublicLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* Atajo para saltar al contenido sin recorrer el menu con el teclado. */}
      <Link
        href="#contenido"
        sx={{
          position: 'absolute',
          left: -9999,
          top: 0,
          zIndex: 2000,
          px: 2,
          py: 1,
          bgcolor: guinda[500],
          color: '#FFFFFF',
          textDecoration: 'none',
          '&:focus': { left: 8, top: 8 },
        }}
      >
        Ir al contenido
      </Link>

      <AppBar position="sticky" sx={{ bgcolor: guinda[500] }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: 2 }}>
            <PetsIcon aria-hidden />
            <Box sx={{ flexGrow: 1 }}>
              <Typography
                component={RouterLink}
                to="/"
                variant="h6"
                sx={{ color: 'inherit', textDecoration: 'none', display: 'block' }}
              >
                Padron Estatal de Perros y Gatos
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.85 }}>
                Instituto de Bienestar Animal
              </Typography>
            </Box>

            <Button component={RouterLink} to="/consulta" color="inherit">
              Consultar tramite
            </Button>
            <Button component={RouterLink} to="/verificar" color="inherit">
              Verificar certificado
            </Button>
            <Button
              component={RouterLink}
              to="/registro"
              variant="outlined"
              sx={{ color: '#FFFFFF', borderColor: 'rgba(255,255,255,0.7)' }}
            >
              Registrar
            </Button>
          </Toolbar>
        </Container>
      </AppBar>

      <Box component="main" id="contenido" sx={{ flexGrow: 1, py: 4 }}>
        <Container maxWidth="lg">
          <Outlet />
        </Container>
      </Box>

      <Box
        component="footer"
        sx={{
          borderTop: `1px solid ${neutro[200]}`,
          bgcolor: '#FFFFFF',
          py: 3,
          mt: 4,
        }}
      >
        <Container maxWidth="lg">
          <Typography variant="body2" color="text.secondary">
            Sistema Estatal DIF Puebla - Instituto de Bienestar Animal
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Tus datos personales se tratan conforme al aviso de privacidad vigente.
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}

export default PublicLayout;
