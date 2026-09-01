import { Chip } from '@mui/material';

import { colorEstatus } from '@/theme';
import type { ClaveEstatus } from '@/types/dominio';

interface Props {
  clave: ClaveEstatus | string;
  nombre: string;
  /** Color que envia el catalogo; el personal puede editarlo. */
  color?: string;
  size?: 'small' | 'medium';
}

/**
 * Insignia con la situacion del tramite.
 *
 * <p>El color viene del catalogo, para que el personal pueda ajustarlo sin
 * tocar codigo. Si no llega, se usa el de la paleta.
 *
 * <p>El color nunca va solo: siempre acompana al texto. Quien no distingue
 * ciertos colores tiene que poder leer en que va el tramite igual.
 */
export function EstatusChip({ clave, nombre, color, size = 'small' }: Props) {
  const colorFinal = color ?? colorEstatus[clave] ?? '#64748B';

  return (
    <Chip
      label={nombre}
      size={size}
      sx={{
        backgroundColor: `${colorFinal}18`,
        color: colorFinal,
        border: `1px solid ${colorFinal}44`,
        fontWeight: 600,
      }}
    />
  );
}

export default EstatusChip;
