import { createTheme } from '@mui/material/styles';
import type { PaletteColorOptions } from '@mui/material/styles';

/**
 * Tema del sistema. Unica fuente de verdad del color y la tipografia.
 *
 * <p>Todo lo demas consume de aqui: ningun componente escribe un color a mano.
 * Asi, cuando la institucion cambie de identidad grafica, se toca un archivo.
 *
 * <p>Los ratios de contraste estan comprobados contra blanco y anotados en cada
 * tono. Importa porque el padron lo usa personal que pasa el dia entero frente
 * a la pantalla, y porque un tramite de gobierno tiene que ser usable por
 * cualquiera.
 */

/**
 * Guinda institucional, en ocho tonos.
 *
 * <p>Los tonos 50 a 300 NO deben usarse como texto: no alcanzan el contraste
 * minimo. Son para fondos, bordes y estados deshabilitados.
 */
export const guinda = {
  50: '#FDF2F5',  // fondo muy suave: filas alternas, hover de lista
  100: '#FAE0E7', // fondo de insignias
  200: '#F0BCC9', // bordes decorativos
  300: '#D98EA3', // deshabilitado
  400: '#B85A76', // 4.41:1 - solo texto grande sobre blanco
  500: '#801A36', // 9.91:1 - principal, cumple AAA
  600: '#6D142E', // superficie guinda al pasar el cursor
  700: '#5C0A28', // 13.73:1 - pulsado y enfasis
  800: '#42071C', // 16.42:1 - maximo contraste
} as const;

/** Neutros para texto, fondos y bordes. */
export const neutro = {
  50: '#F8FAFC',  // fondo de pagina
  100: '#F1F5F9', // cabecera de tabla, campo deshabilitado
  200: '#E2E8F0', // bordes y divisores
  300: '#CBD5E1', // borde de campo
  400: '#94A3B8', // texto deshabilitado y marcador de posicion
  500: '#64748B', // 4.76:1 - texto secundario
  600: '#475569', // 7.58:1 - texto secundario sobre fondo claro
  700: '#334155', // secundario
  800: '#1E293B', // fondo de sugerencias
  900: '#0F172A', // texto principal
} as const;

/**
 * Colores de estado.
 *
 * <p>Cada tono principal supera 5:1 sobre blanco. El claro es para fondos de
 * aviso y el oscuro para el texto que va encima.
 */
export const estado = {
  exito: { main: '#15803D', light: '#DCFCE7', dark: '#166534' },   // 5.02:1
  error: { main: '#C62828', light: '#FEE2E2', dark: '#991B1B' },   // 5.62:1
  aviso: { main: '#B45309', light: '#FEF3C7', dark: '#92400E' },   // 5.02:1
  info: { main: '#0369A1', light: '#E0F2FE', dark: '#075985' },    // 5.93:1
} as const;

/**
 * Colores de los estados del tramite.
 *
 * <p>Coinciden con los que trae el catalogo, que el personal puede editar. Esto
 * es el respaldo cuando la pantalla todavia no los recibio.
 */
export const colorEstatus: Record<string, string> = {
  REGISTRADA: estado.info.main,
  EN_REVISION: estado.aviso.main,
  OBSERVADA: estado.aviso.main,
  APROBADA: estado.exito.main,
  CERTIFICADO_EMITIDO: estado.exito.main,
  ENTREGADA: estado.exito.dark,
  RECHAZADA: estado.error.main,
  CANCELADA: neutro[500],
};

const colorPrimario: PaletteColorOptions = {
  light: guinda[400],
  main: guinda[500],
  dark: guinda[700],
  contrastText: '#FFFFFF',
};

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: colorPrimario,
    secondary: {
      light: neutro[500],
      main: neutro[700],
      dark: neutro[900],
      contrastText: '#FFFFFF',
    },
    success: { ...estado.exito, contrastText: '#FFFFFF' },
    error: { ...estado.error, contrastText: '#FFFFFF' },
    warning: { ...estado.aviso, contrastText: '#FFFFFF' },
    info: { ...estado.info, contrastText: '#FFFFFF' },
    background: {
      default: neutro[50],
      paper: '#FFFFFF',
    },
    text: {
      primary: neutro[900],
      secondary: neutro[600],
      disabled: neutro[400],
    },
    divider: neutro[200],
  },

  typography: {
    fontFamily: [
      'Inter',
      '-apple-system',
      'BlinkMacSystemFont',
      'Segoe UI',
      'Roboto',
      'sans-serif',
    ].join(','),
    // Base de 16px: por debajo de eso el texto cansa en jornadas largas.
    fontSize: 16,
    h1: { fontSize: '2.5rem', fontWeight: 700, lineHeight: 1.2 },
    h2: { fontSize: '2rem', fontWeight: 600, lineHeight: 1.25 },
    h3: { fontSize: '1.75rem', fontWeight: 600, lineHeight: 1.3 },
    h4: { fontSize: '1.5rem', fontWeight: 600, lineHeight: 1.3 },
    h5: { fontSize: '1.25rem', fontWeight: 600, lineHeight: 1.4 },
    h6: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.4 },
    body1: { fontSize: '1rem', lineHeight: 1.6 },
    body2: { fontSize: '0.875rem', lineHeight: 1.5 },
    button: { fontWeight: 500, textTransform: 'none' },
  },

  shape: {
    borderRadius: 8,
  },

  // Escala de 8px: los margenes se derivan de ella y no se inventan por pantalla.
  spacing: 8,

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        // Quien navega con teclado tiene que ver donde esta parado.
        '*:focus-visible': {
          outline: `3px solid ${guinda[500]}`,
          outlineOffset: '2px',
        },
        // Se respeta a quien pidio menos movimiento en su sistema.
        '@media (prefers-reduced-motion: reduce)': {
          '*': {
            animationDuration: '0.01ms !important',
            transitionDuration: '0.01ms !important',
          },
        },
      },
    },

    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          // 44px: lo minimo para poder tocarlo con el dedo sin fallar.
          minHeight: 44,
          paddingInline: 20,
        },
        sizeSmall: { minHeight: 36 },
      },
    },

    MuiTextField: {
      defaultProps: { variant: 'outlined', size: 'medium' },
    },

    MuiCard: {
      styleOverrides: {
        root: {
          border: `1px solid ${neutro[200]}`,
          boxShadow: '0 1px 3px rgba(15, 23, 42, 0.06)',
        },
      },
    },

    MuiTableHead: {
      styleOverrides: {
        root: { backgroundColor: neutro[100] },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 500 },
      },
    },

    MuiAppBar: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { borderBottom: `1px solid ${neutro[200]}` },
      },
    },
  },
});

export default theme;
