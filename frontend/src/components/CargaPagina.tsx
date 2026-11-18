import { Box, CircularProgress } from '@mui/material';

/**
 * Indicador que se muestra mientras se descarga el código de una pantalla.
 * Área: Núcleo compartido. Lógica: carga diferida de rutas (React.lazy en App.tsx).
 *
 * <p>Cada pantalla se descarga solo cuando se visita: el portal ciudadano no baja el código del
 * panel administrativo. Este indicador ocupa el lugar de la pantalla durante esa descarga, sin
 * ocultar el encabezado ni el menú del layout.
 *
 * @returns {JSX.Element}
 */
export function CargaPagina() {
  return (
    <Box
      role="status"
      aria-live="polite"
      sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 8 }}
    >
      <CircularProgress aria-label="Cargando la pantalla" />
    </Box>
  );
}

export default CargaPagina;
