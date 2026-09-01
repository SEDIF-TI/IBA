import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';

interface Props {
  abierto: boolean;
  titulo: string;
  mensaje: string;
  textoConfirmar?: string;
  textoCancelar?: string;
  /** Tine el boton de rojo, para acciones que no se deshacen. */
  peligroso?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}

/**
 * Confirmacion antes de una accion que cuesta deshacer.
 *
 * <p>El boton de confirmar recibe el foco al abrirse, para poder responder con
 * el teclado sin ir buscando.
 */
export function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = 'Confirmar',
  textoCancelar = 'Cancelar',
  peligroso = false,
  onConfirmar,
  onCancelar,
}: Props) {
  return (
    <Dialog
      open={abierto}
      onClose={onCancelar}
      aria-labelledby="titulo-confirmacion"
      aria-describedby="mensaje-confirmacion"
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle id="titulo-confirmacion">{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText id="mensaje-confirmacion">{mensaje}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        {/* Cancelar va primero y con menos peso visual: no debe pulsarse por inercia. */}
        <Button onClick={onCancelar} color="inherit">
          {textoCancelar}
        </Button>
        <Button
          onClick={onConfirmar}
          variant="contained"
          color={peligroso ? 'error' : 'primary'}
          autoFocus
        >
          {textoConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ConfirmDialog;
