import {
  AppBar,
  Avatar,
  Box,
  Chip,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import CategoryIcon from '@mui/icons-material/Category';
import EventIcon from '@mui/icons-material/Event';
import PhoneIcon from '@mui/icons-material/Phone';
import DashboardIcon from '@mui/icons-material/Dashboard';
import LogoutIcon from '@mui/icons-material/Logout';
import MenuIcon from '@mui/icons-material/Menu';
import PeopleIcon from '@mui/icons-material/People';
import PetsIcon from '@mui/icons-material/Pets';
import { useEffect, useState } from 'react';
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '@/context/AuthContext';
import { useNotificacion } from '@/context/NotificationContext';
import notificacionService from '@/services/notificacionService';
import { guinda, neutro } from '@/theme';

const ANCHO_MENU = 260;

/** Una entrada del menu lateral. */
interface EntradaMenu {
  texto: string;
  ruta: string;
  icono: React.ReactNode;
  /** Permiso necesario para verla. */
  permiso?: string;
}

const ENTRADAS: EntradaMenu[] = [
  { texto: 'Tablero', ruta: '/admin', icono: <DashboardIcon /> },
  {
    texto: 'Tramites',
    ruta: '/admin/solicitudes',
    icono: <AssignmentIcon />,
    permiso: 'solicitud:ver',
  },
  {
    texto: 'Jornadas',
    ruta: '/admin/jornadas',
    icono: <EventIcon />,
    permiso: 'jornada:ver',
  },
  {
    texto: 'Bandeja de contacto',
    ruta: '/admin/contacto',
    icono: <PhoneIcon />,
    permiso: 'notificacion:contactar',
  },
  {
    texto: 'Padron',
    ruta: '/admin/padron',
    icono: <PetsIcon />,
    permiso: 'animal:ver',
  },
  {
    texto: 'Tutores',
    ruta: '/admin/tutores',
    icono: <PeopleIcon />,
    permiso: 'persona:ver',
  },
  {
    texto: 'Catalogos',
    ruta: '/admin/catalogos',
    icono: <CategoryIcon />,
    permiso: 'catalogo:ver',
  },
];

/**
 * Estructura del panel administrativo.
 *
 * <p>El menu solo muestra lo que la persona puede usar. Es por claridad, no por
 * seguridad: quien no tiene el permiso tampoco pasa del backend aunque escriba
 * la direccion a mano.
 */
export function AdminLayout() {
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [anclaUsuario, setAnclaUsuario] = useState<null | HTMLElement>(null);
  const [contactosPendientes, setContactosPendientes] = useState(0);

  const { sesion, cerrarSesion, tienePermiso } = useAuth();
  const notificacion = useNotificacion();
  const navegar = useNavigate();
  const ubicacion = useLocation();

  const entradasVisibles = ENTRADAS.filter(
    (entrada) => !entrada.permiso || tienePermiso(entrada.permiso),
  );

  // El contador de contactos pendientes va en el menu para que nadie quede
  // esperando una llamada que nadie recuerda que debe hacer.
  useEffect(() => {
    if (!tienePermiso('notificacion:ver')) {
      return;
    }
    notificacionService
      .pendientes()
      .then((resumen) => setContactosPendientes(resumen.pendientes))
      .catch(() => setContactosPendientes(0));
  }, [tienePermiso, ubicacion.pathname]);

  async function salir() {
    try {
      await cerrarSesion();
      navegar('/admin/login', { replace: true });
    } catch {
      notificacion.error('No se pudo cerrar la sesion');
    }
  }

  const contenidoMenu = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ gap: 1.5, bgcolor: guinda[500], color: '#FFFFFF' }}>
        <PetsIcon aria-hidden />
        <Box>
          <Typography variant="subtitle1" fontWeight={600} lineHeight={1.2}>
            SEA
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.85 }}>
            Panel administrativo
          </Typography>
        </Box>
      </Toolbar>
      <Divider />

      <List component="nav" sx={{ flexGrow: 1, py: 1 }}>
        {entradasVisibles.map((entrada) => {
          const activa =
            entrada.ruta === '/admin'
              ? ubicacion.pathname === '/admin'
              : ubicacion.pathname.startsWith(entrada.ruta);

          return (
            <ListItemButton
              key={entrada.ruta}
              component={RouterLink}
              to={entrada.ruta}
              selected={activa}
              onClick={() => setMenuAbierto(false)}
              sx={{
                mx: 1,
                borderRadius: 1,
                '&.Mui-selected': {
                  bgcolor: guinda[50],
                  color: guinda[700],
                  '& .MuiListItemIcon-root': { color: guinda[700] },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>{entrada.icono}</ListItemIcon>
              <ListItemText primary={entrada.texto} />
              {entrada.ruta === '/admin/contacto' && contactosPendientes > 0 && (
                <Chip
                  label={contactosPendientes}
                  size="small"
                  color="warning"
                  sx={{ height: 20, minWidth: 20 }}
                />
              )}
            </ListItemButton>
          );
        })}
      </List>

      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          {sesion?.roles.join(', ')}
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${ANCHO_MENU}px)` },
          ml: { md: `${ANCHO_MENU}px` },
          bgcolor: '#FFFFFF',
          color: neutro[900],
        }}
      >
        <Toolbar>
          <IconButton
            edge="start"
            onClick={() => setMenuAbierto(true)}
            sx={{ mr: 2, display: { md: 'none' } }}
            aria-label="Abrir menu"
          >
            <MenuIcon />
          </IconButton>

          <Box sx={{ flexGrow: 1 }} />

          <Tooltip title="Cuenta">
            <IconButton
              onClick={(evento) => setAnclaUsuario(evento.currentTarget)}
              aria-label="Menu de la cuenta"
            >
              <Avatar sx={{ width: 36, height: 36, bgcolor: guinda[500], fontSize: 15 }}>
                {sesion?.nombreCompleto?.charAt(0) ?? '?'}
              </Avatar>
            </IconButton>
          </Tooltip>

          <Menu
            anchorEl={anclaUsuario}
            open={Boolean(anclaUsuario)}
            onClose={() => setAnclaUsuario(null)}
          >
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="subtitle2">{sesion?.nombreCompleto}</Typography>
              <Typography variant="caption" color="text.secondary">
                {sesion?.usuario}
              </Typography>
            </Box>
            <Divider />
            <MenuItem
              onClick={() => {
                setAnclaUsuario(null);
                navegar('/admin/cambiar-password');
              }}
            >
              Cambiar contrasena
            </MenuItem>
            <MenuItem onClick={salir}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Cerrar sesion
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: ANCHO_MENU }, flexShrink: { md: 0 } }}>
        {/* En pantalla chica el menu se abre por encima; en grande queda fijo. */}
        <Drawer
          variant="temporary"
          open={menuAbierto}
          onClose={() => setMenuAbierto(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: ANCHO_MENU },
          }}
        >
          {contenidoMenu}
        </Drawer>

        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': {
              width: ANCHO_MENU,
              borderRight: `1px solid ${neutro[200]}`,
            },
          }}
        >
          {contenidoMenu}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${ANCHO_MENU}px)` },
          p: { xs: 2, md: 3 },
          mt: 8,
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}

export default AdminLayout;
