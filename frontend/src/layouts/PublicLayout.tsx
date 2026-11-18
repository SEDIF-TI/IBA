import {
  alpha,
  AppBar,
  Box,
  Button,
  Container,
  IconButton,
  Link,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import PetsIcon from '@mui/icons-material/Pets';
import { Suspense, useState } from 'react';
import { Link as RouterLink, Outlet } from 'react-router-dom';

import CargaPagina from '@/components/CargaPagina';

import { guinda, neutro } from '@/theme';

/** Enlaces de navegacion del portal, en el orden en que se muestran. */
const ENLACES = [
  { ruta: '/consulta', texto: 'Consultar trámite' },
  { ruta: '/jornadas', texto: 'Jornadas' },
  { ruta: '/verificar', texto: 'Verificar' },
];

/**
 * Estructura del portal ciudadano.
 * Area: Nucleo compartido (portal publico).
 *
 * <p>Sin sesion ni menu de usuario: aqui no hay cuentas. La ciudadania entra,
 * hace su tramite y se va con su folio.
 *
 * <p>Responsivo: desde md los enlaces van en la barra; en celular y tablet
 * vertical pasan a un menu, para que la barra no se desborde a lo ancho. El
 * boton "Registrar" queda siempre visible porque es la accion principal.
 *
 * <p>Estado propio: el ancla del menu de navegacion en pantallas chicas.
 *
 * @returns {JSX.Element}
 */
export function PublicLayout() {
  const [anclaMenu, setAnclaMenu] = useState<HTMLElement | null>(null);

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
          color: 'common.white',
          textDecoration: 'none',
          '&:focus': { left: 8, top: 8 },
        }}
      >
        Ir al contenido
      </Link>

      <AppBar position="sticky" sx={{ bgcolor: guinda[500] }}>
        <Container maxWidth="lg">
          <Toolbar disableGutters sx={{ gap: { xs: 1, md: 2 }, py: { xs: 1, md: 0 } }}>
            <PetsIcon aria-hidden sx={{ display: { xs: 'none', sm: 'block' } }} />
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography
                component={RouterLink}
                to="/"
                variant="h6"
                sx={{
                  color: 'inherit',
                  textDecoration: 'none',
                  display: 'block',
                  fontSize: { xs: '1rem', sm: '1.25rem' },
                  lineHeight: 1.25,
                }}
              >
                Padrón Estatal de Perros y Gatos
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.85 }}>
                Instituto de Bienestar Animal
              </Typography>
            </Box>

            {ENLACES.map((enlace) => (
              <Button
                key={enlace.ruta}
                component={RouterLink}
                to={enlace.ruta}
                color="inherit"
                sx={{ display: { xs: 'none', md: 'inline-flex' } }}
              >
                {enlace.texto}
              </Button>
            ))}
            <Button
              component={RouterLink}
              to="/registro"
              variant="outlined"
              sx={{ color: 'common.white', borderColor: (t) => alpha(t.palette.common.white, 0.7) }}
            >
              Registrar
            </Button>

            {/* Celular y tablet vertical: los enlaces van en un menu (objetivo tactil de 44 px). */}
            <IconButton
              color="inherit"
              aria-label="Abrir menú de navegación"
              aria-haspopup="true"
              onClick={(evento) => setAnclaMenu(evento.currentTarget)}
              sx={{ display: { xs: 'inline-flex', md: 'none' }, width: '2.75rem', height: '2.75rem' }}
            >
              <MenuIcon />
            </IconButton>
            <Menu
              anchorEl={anclaMenu}
              open={Boolean(anclaMenu)}
              onClose={() => setAnclaMenu(null)}
            >
              {ENLACES.map((enlace) => (
                <MenuItem
                  key={enlace.ruta}
                  component={RouterLink}
                  to={enlace.ruta}
                  onClick={() => setAnclaMenu(null)}
                  sx={{ minHeight: '2.75rem' }}
                >
                  {enlace.texto}
                </MenuItem>
              ))}
            </Menu>
          </Toolbar>
        </Container>
      </AppBar>

      <Box component="main" id="contenido" sx={{ flexGrow: 1, py: 4 }}>
        <Container maxWidth="lg">
          <Suspense fallback={<CargaPagina />}>
            <Outlet />
          </Suspense>
        </Container>
      </Box>

      <Box
        component="footer"
        sx={{
          borderTop: `1px solid ${neutro[200]}`,
          bgcolor: 'background.paper',
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
