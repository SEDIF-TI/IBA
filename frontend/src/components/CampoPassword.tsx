import { IconButton, InputAdornment, TextField, type TextFieldProps } from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { useState } from 'react';

/**
 * Campo de contraseña con el botón del "ojito" para mostrar u ocultar lo escrito.
 * Área: Núcleo compartido (acceso). Lógica: inicio de sesión y cambio de contraseña.
 *
 * <p>Permite revisar que la contraseña se escribió bien antes de enviarla, sin dejarla visible por
 * omisión: arranca oculta y vuelve a ocultarse cuando se desmonta el campo. El botón mide 44 px
 * (objetivo táctil) y su etiqueta accesible dice qué hará al pulsarlo.
 *
 * <p>Estado propio: si la contraseña se está mostrando.
 *
 * @param {TextFieldProps} props - las mismas del TextField de Material UI; `type` se controla aquí
 * @returns {JSX.Element}
 */
export function CampoPassword(props: Omit<TextFieldProps, 'type'>) {
  const [visible, setVisible] = useState(false);
  const { InputProps, ...resto } = props;

  return (
    <TextField
      {...resto}
      type={visible ? 'text' : 'password'}
      InputProps={{
        ...InputProps,
        endAdornment: (
          <InputAdornment position="end">
            <IconButton
              aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              aria-pressed={visible}
              onClick={() => setVisible((actual) => !actual)}
              // Evita que el campo pierda el foco al pulsar el botón con el ratón.
              onMouseDown={(evento) => evento.preventDefault()}
              edge="end"
              disabled={props.disabled}
              sx={{ width: '2.75rem', height: '2.75rem' }}
            >
              {visible ? <VisibilityOffIcon /> : <VisibilityIcon />}
            </IconButton>
          </InputAdornment>
        ),
      }}
    />
  );
}

export default CampoPassword;
