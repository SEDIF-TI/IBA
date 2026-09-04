import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useNotificacion } from '@/context/NotificationContext';
import { mensajeDeError } from '@/services/api';
import { catalogos } from '@/services/catalogoService';
import jornadaService from '@/services/jornadaService';
import solicitudService, {
  type RegistroPublicoRequest,
  type RegistroRespuesta,
} from '@/services/solicitudService';
import { guinda } from '@/theme';
import type { Catalogo, JornadaPublica, Municipio, Raza } from '@/types/dominio';

const PASOS = ['Tus datos', 'Tu animal', 'Confirmar'];

/** Estado inicial del formulario. */
const VACIO = {
  curp: '',
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  correo: '',
  telefono: '',
  municipioId: 0,
  calle: '',
  numeroExterior: '',
  colonia: '',
  codigoPostal: '',
  animalNombre: '',
  especieId: 0,
  razaId: 0,
  colorId: 0,
  sexoId: 0,
  tamanoId: 0,
  edadAnios: '',
  edadMeses: '',
  vacunado: false,
  esterilizado: false,
  caracteristicas: '',
};

/**
 * Registro de un tramite desde el portal, sin cuenta de usuario.
 *
 * <p>Va por pasos para no abrumar con un formulario largo: primero quien
 * responde por el animal, luego el animal, y al final la confirmacion con el
 * aviso de privacidad.
 *
 * <p>El sistema anterior obligaba a crear cuenta y esperar hasta 24 horas a que
 * alguien la validara antes siquiera de poder registrar al animal. Buena parte
 * de la gente abandonaba ahi.
 */
interface Props {
  /**
   * Cuando es verdadero, el personal captura por la persona desde el panel.
   *
   * <p>El formulario es el mismo; cambian los textos y el destino, porque quien
   * escribe no es la persona sino quien la atiende.
   */
  ventanilla?: boolean;
  /** Se invoca al terminar, con el folio generado. */
  alTerminar?: (folio: string, solicitudId: number) => void;
}

export function RegistroTramite({ ventanilla = false, alTerminar }: Props = {}) {
  const [parametros] = useSearchParams();
  const jornadaId = parametros.get('jornada');

  const [paso, setPaso] = useState(0);
  const [datos, setDatos] = useState(VACIO);
  const [enviando, setEnviando] = useState(false);
  const [aceptaAviso, setAceptaAviso] = useState(false);
  const [resultado, setResultado] = useState<RegistroRespuesta | null>(null);

  const [municipios, setMunicipios] = useState<Municipio[]>([]);
  const [especies, setEspecies] = useState<Catalogo[]>([]);
  const [razas, setRazas] = useState<Raza[]>([]);
  const [colores, setColores] = useState<Catalogo[]>([]);
  const [sexos, setSexos] = useState<Catalogo[]>([]);
  const [tamanos, setTamanos] = useState<Catalogo[]>([]);
  const [jornada, setJornada] = useState<JornadaPublica | null>(null);

  const notificacion = useNotificacion();

  useEffect(() => {
    async function cargarCatalogos() {
      try {
        const [mun, esp, col, sex, tam] = await Promise.all([
          catalogos.municipios.opciones() as Promise<Municipio[]>,
          catalogos.especies.opciones(),
          catalogos.colores.opciones(),
          catalogos.sexos.opciones(),
          catalogos.tamanos.opciones(),
        ]);
        setMunicipios(mun);
        setEspecies(esp);
        setColores(col);
        setSexos(sex);
        setTamanos(tam);
      } catch (fallo) {
        notificacion.error(mensajeDeError(fallo));
      }
    }
    void cargarCatalogos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Si viene de una jornada, se muestra a cual se esta adelantando el registro.
  useEffect(() => {
    if (!jornadaId) return;
    jornadaService
      .calendarioPublico()
      .then((lista) => setJornada(lista.find((j) => j.id === Number(jornadaId)) ?? null))
      .catch(() => setJornada(null));
  }, [jornadaId]);

  // Las razas dependen de la especie: un gato no puede ser Chihuahua.
  useEffect(() => {
    if (!datos.especieId) {
      setRazas([]);
      return;
    }
    catalogos.razas
      .opciones()
      .then((todas) =>
        setRazas((todas as Raza[]).filter((r) => r.especieId === datos.especieId)),
      )
      .catch(() => setRazas([]));
  }, [datos.especieId]);

  function actualizar<K extends keyof typeof VACIO>(campo: K, valor: (typeof VACIO)[K]) {
    setDatos((anterior) => ({ ...anterior, [campo]: valor }));
  }

  /** Comprueba que el paso actual tenga lo indispensable. */
  function pasoCompleto(indice: number): boolean {
    if (indice === 0) {
      const tieneContacto = datos.correo.trim() !== '' || datos.telefono.trim() !== '';
      return (
        datos.nombre.trim() !== '' &&
        datos.apellidoPaterno.trim() !== '' &&
        datos.municipioId > 0 &&
        tieneContacto
      );
    }
    if (indice === 1) {
      return datos.animalNombre.trim() !== '' && datos.especieId > 0 && datos.sexoId > 0;
    }
    return aceptaAviso;
  }

  async function enviar() {
    setEnviando(true);
    try {
      const peticion: RegistroPublicoRequest = {
        tutor: {
          curp: datos.curp.trim() || undefined,
          nombre: datos.nombre.trim(),
          apellidoPaterno: datos.apellidoPaterno.trim(),
          apellidoMaterno: datos.apellidoMaterno.trim() || undefined,
          correo: datos.correo.trim() || undefined,
          telefono: datos.telefono.trim() || undefined,
          municipioId: datos.municipioId,
          calle: datos.calle.trim() || undefined,
          numeroExterior: datos.numeroExterior.trim() || undefined,
          colonia: datos.colonia.trim() || undefined,
          codigoPostal: datos.codigoPostal.trim() || undefined,
        },
        animal: {
          nombre: datos.animalNombre.trim(),
          especieId: datos.especieId,
          razaId: datos.razaId || undefined,
          colorId: datos.colorId || undefined,
          sexoId: datos.sexoId,
          tamanoId: datos.tamanoId || undefined,
          edadAnios: datos.edadAnios ? parseInt(datos.edadAnios, 10) : undefined,
          edadMeses: datos.edadMeses ? parseInt(datos.edadMeses, 10) : undefined,
          vacunado: datos.vacunado,
          esterilizado: datos.esterilizado,
          caracteristicas: datos.caracteristicas.trim() || undefined,
        },
        aceptaAviso: true,
        avisoVersion: '1.0',
        jornadaId: jornadaId ? Number(jornadaId) : undefined,
      };

      if (ventanilla) {
        const { solicitud, mensaje } = await solicitudService.capturarEnVentanilla(peticion);
        notificacion.exito(mensaje);
        alTerminar?.(solicitud.folio, solicitud.id);
        setResultado({
          folio: solicitud.folio,
          token: '',
          estatus: solicitud.estatusNombre,
          mensaje:
            'Entrega este folio a la persona. Con el y su CURP puede consultar ' +
            'el avance de su tramite.',
        });
      } else {
        setResultado(await solicitudService.registrarPublico(peticion));
      }
    } catch (fallo) {
      notificacion.error(mensajeDeError(fallo));
    } finally {
      setEnviando(false);
    }
  }

  // ---- Comprobante del registro ----
  if (resultado) {
    return (
      <Box sx={{ maxWidth: 620, mx: 'auto' }}>
        <Card>
          <CardContent sx={{ p: 4, textAlign: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 56, color: 'success.main' }} aria-hidden />
            <Typography variant="h5" fontWeight={600} sx={{ mt: 1 }} gutterBottom>
              {ventanilla ? 'Tramite capturado' : 'Tu tramite quedo registrado'}
            </Typography>

            <Box
              sx={{
                my: 3,
                p: 2.5,
                bgcolor: guinda[50],
                border: 1,
                borderColor: guinda[200],
                borderRadius: 1,
              }}
            >
              <Typography variant="overline" color="text.secondary">
                Folio de seguimiento
              </Typography>
              <Typography variant="h5" fontFamily="monospace" color={guinda[700]}>
                {resultado.folio}
              </Typography>
            </Box>

            <Alert severity="warning" sx={{ textAlign: 'left', mb: 2 }}>
              <Typography variant="subtitle2">
                {ventanilla ? 'Entrega este folio a la persona' : 'Anota tu folio'}
              </Typography>
              <Typography variant="body2">{resultado.mensaje}</Typography>
            </Alert>

            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Con tu folio y tu CURP puedes consultar en cualquier momento en que
              va tu tramite.
            </Typography>

            {ventanilla ? (
              <Button
                variant="contained"
                onClick={() => {
                  setResultado(null);
                  setDatos(VACIO);
                  setAceptaAviso(false);
                  setPaso(0);
                }}
              >
                Capturar otro tramite
              </Button>
            ) : (
              <>
                <Button href="/consulta" variant="contained" sx={{ mr: 1 }}>
                  Consultar mi tramite
                </Button>
                <Button href="/" color="inherit">
                  Volver al inicio
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 720, mx: 'auto' }}>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        {ventanilla ? 'Capturar tramite en ventanilla' : 'Registrar a mi perro o gato'}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {ventanilla
          ? 'Captura los datos que te proporciona la persona. Al terminar, '
            + 'entregale el folio para que pueda dar seguimiento.'
          : 'No necesitas crear una cuenta. Al terminar recibes un folio para '
            + 'dar seguimiento.'}
      </Typography>

      {jornada && (
        <Alert severity="info" sx={{ mb: 2 }}>
          <Typography variant="subtitle2">
            Estas adelantando tu registro para: {jornada.nombre}
          </Typography>
          <Typography variant="body2">
            {jornada.municipio} - {jornada.domicilioSede}
            {jornada.horario ? ` - ${jornada.horario}` : ''}
          </Typography>
        </Alert>
      )}

      <Stepper activeStep={paso} sx={{ mb: 3 }}>
        {PASOS.map((etiqueta) => (
          <Step key={etiqueta}>
            <StepLabel>{etiqueta}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <Card>
        <CardContent sx={{ p: 3 }}>
          {paso === 0 && (
            <>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                {ventanilla ? 'Datos del tutor responsable' : 'Tus datos'}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {ventanilla
                  ? 'Quien responde por el animal.'
                  : 'Eres el tutor responsable del animal.'}
              </Typography>

              <TextField
                label="CURP"
                value={datos.curp}
                onChange={(e) => actualizar('curp', e.target.value.toUpperCase())}
                fullWidth
                margin="normal"
                inputProps={{ maxLength: 18, style: { fontFamily: 'monospace' } }}
                helperText="Opcional. Si no la tienes a la mano puedes continuar sin ella."
              />

              <Grid container spacing={2}>
                <Grid item xs={12} sm={4}>
                  <TextField
                    label="Nombre(s)"
                    value={datos.nombre}
                    onChange={(e) => actualizar('nombre', e.target.value)}
                    fullWidth
                    required
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    label="Apellido paterno"
                    value={datos.apellidoPaterno}
                    onChange={(e) => actualizar('apellidoPaterno', e.target.value)}
                    fullWidth
                    required
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} sm={4}>
                  <TextField
                    label="Apellido materno"
                    value={datos.apellidoMaterno}
                    onChange={(e) => actualizar('apellidoMaterno', e.target.value)}
                    fullWidth
                    margin="normal"
                  />
                </Grid>
              </Grid>

              <Alert severity="info" sx={{ my: 2 }}>
                Deja al menos un correo o un telefono: es por donde te avisamos el
                resultado de tu tramite.
              </Alert>

              <Grid container spacing={2}>
                <Grid item xs={12} sm={7}>
                  <TextField
                    label="Correo electronico"
                    type="email"
                    value={datos.correo}
                    onChange={(e) => actualizar('correo', e.target.value)}
                    fullWidth
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={12} sm={5}>
                  <TextField
                    label="Telefono"
                    value={datos.telefono}
                    onChange={(e) =>
                      actualizar('telefono', e.target.value.replace(/\D/g, '').slice(0, 10))
                    }
                    fullWidth
                    margin="normal"
                    helperText="10 digitos"
                  />
                </Grid>
              </Grid>

              <FormControl fullWidth margin="normal" required>
                <InputLabel id="etiqueta-municipio-registro">Municipio</InputLabel>
                <Select
                  labelId="etiqueta-municipio-registro"
                  label="Municipio"
                  value={datos.municipioId || ''}
                  onChange={(e) => actualizar('municipioId', Number(e.target.value))}
                >
                  {municipios.map((m) => (
                    <MenuItem key={m.id} value={m.id}>
                      {m.nombre}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Grid container spacing={2}>
                <Grid item xs={8}>
                  <TextField
                    label="Calle"
                    value={datos.calle}
                    onChange={(e) => actualizar('calle', e.target.value)}
                    fullWidth
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={4}>
                  <TextField
                    label="Numero"
                    value={datos.numeroExterior}
                    onChange={(e) => actualizar('numeroExterior', e.target.value)}
                    fullWidth
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={8}>
                  <TextField
                    label="Colonia"
                    value={datos.colonia}
                    onChange={(e) => actualizar('colonia', e.target.value)}
                    fullWidth
                    margin="normal"
                  />
                </Grid>
                <Grid item xs={4}>
                  <TextField
                    label="Codigo postal"
                    value={datos.codigoPostal}
                    onChange={(e) =>
                      actualizar('codigoPostal', e.target.value.replace(/\D/g, '').slice(0, 5))
                    }
                    fullWidth
                    margin="normal"
                  />
                </Grid>
              </Grid>
            </>
          )}

          {paso === 1 && (
            <>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Datos de tu animal
              </Typography>

              <TextField
                label="Como se llama"
                value={datos.animalNombre}
                onChange={(e) => actualizar('animalNombre', e.target.value)}
                fullWidth
                required
                margin="normal"
              />

              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth margin="normal" required>
                    <InputLabel id="etiqueta-especie">Especie</InputLabel>
                    <Select
                      labelId="etiqueta-especie"
                      label="Especie"
                      value={datos.especieId || ''}
                      onChange={(e) => {
                        actualizar('especieId', Number(e.target.value));
                        // La raza depende de la especie: se limpia al cambiarla.
                        actualizar('razaId', 0);
                      }}
                    >
                      {especies.map((e) => (
                        <MenuItem key={e.id} value={e.id}>
                          {e.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth margin="normal" disabled={!datos.especieId}>
                    <InputLabel id="etiqueta-raza">Raza</InputLabel>
                    <Select
                      labelId="etiqueta-raza"
                      label="Raza"
                      value={datos.razaId || ''}
                      onChange={(e) => actualizar('razaId', Number(e.target.value))}
                    >
                      {razas.map((r) => (
                        <MenuItem key={r.id} value={r.id}>
                          {r.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={6} sm={4}>
                  <FormControl fullWidth margin="normal" required>
                    <InputLabel id="etiqueta-sexo">Sexo</InputLabel>
                    <Select
                      labelId="etiqueta-sexo"
                      label="Sexo"
                      value={datos.sexoId || ''}
                      onChange={(e) => actualizar('sexoId', Number(e.target.value))}
                    >
                      {sexos.map((s) => (
                        <MenuItem key={s.id} value={s.id}>
                          {s.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={6} sm={4}>
                  <FormControl fullWidth margin="normal">
                    <InputLabel id="etiqueta-color">Color</InputLabel>
                    <Select
                      labelId="etiqueta-color"
                      label="Color"
                      value={datos.colorId || ''}
                      onChange={(e) => actualizar('colorId', Number(e.target.value))}
                    >
                      {colores.map((c) => (
                        <MenuItem key={c.id} value={c.id}>
                          {c.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} sm={4}>
                  <FormControl fullWidth margin="normal">
                    <InputLabel id="etiqueta-tamano">Tamano</InputLabel>
                    <Select
                      labelId="etiqueta-tamano"
                      label="Tamano"
                      value={datos.tamanoId || ''}
                      onChange={(e) => actualizar('tamanoId', Number(e.target.value))}
                    >
                      {tamanos.map((t) => (
                        <MenuItem key={t.id} value={t.id}>
                          {t.nombre}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={6}>
                  <TextField
                    label="Anos"
                    type="number"
                    value={datos.edadAnios}
                    onChange={(e) => actualizar('edadAnios', e.target.value)}
                    fullWidth
                    margin="normal"
                    inputProps={{ min: 0, max: 50 }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    label="Meses"
                    type="number"
                    value={datos.edadMeses}
                    onChange={(e) => actualizar('edadMeses', e.target.value)}
                    fullWidth
                    margin="normal"
                    inputProps={{ min: 0, max: 11 }}
                    helperText="De 0 a 11"
                  />
                </Grid>
              </Grid>

              <Box sx={{ my: 1 }}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={datos.vacunado}
                      onChange={(e) => actualizar('vacunado', e.target.checked)}
                    />
                  }
                  label="Esta vacunado"
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={datos.esterilizado}
                      onChange={(e) => actualizar('esterilizado', e.target.checked)}
                    />
                  }
                  label="Esta esterilizado"
                />
              </Box>

              <TextField
                label="Senas particulares"
                value={datos.caracteristicas}
                onChange={(e) => actualizar('caracteristicas', e.target.value)}
                fullWidth
                multiline
                rows={2}
                margin="normal"
                helperText="Manchas, cicatrices, algo que ayude a identificarlo"
              />
            </>
          )}

          {paso === 2 && (
            <>
              <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                Confirma tus datos
              </Typography>

              <Box sx={{ my: 2 }}>
                <Typography variant="overline" color="text.secondary">
                  Tutor responsable
                </Typography>
                <Typography variant="body1">
                  {[datos.nombre, datos.apellidoPaterno, datos.apellidoMaterno]
                    .filter(Boolean)
                    .join(' ')}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {[datos.correo, datos.telefono].filter(Boolean).join(' - ')}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {municipios.find((m) => m.id === datos.municipioId)?.nombre}
                </Typography>
              </Box>

              <Divider />

              <Box sx={{ my: 2 }}>
                <Typography variant="overline" color="text.secondary">
                  Animal
                </Typography>
                <Typography variant="body1">{datos.animalNombre}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {[
                    especies.find((e) => e.id === datos.especieId)?.nombre,
                    razas.find((r) => r.id === datos.razaId)?.nombre,
                    sexos.find((s) => s.id === datos.sexoId)?.nombre,
                    colores.find((c) => c.id === datos.colorId)?.nombre,
                  ]
                    .filter(Boolean)
                    .join(' - ')}
                </Typography>
                <Box sx={{ mt: 0.5 }}>
                  {datos.vacunado && (
                    <Chip label="Vacunado" size="small" sx={{ mr: 0.5 }} />
                  )}
                  {datos.esterilizado && <Chip label="Esterilizado" size="small" />}
                </Box>
              </Box>

              <Divider sx={{ mb: 2 }} />

              <Alert severity="info" sx={{ mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  Aviso de privacidad
                </Typography>
                <Typography variant="body2">
                  El Sistema Estatal DIF Puebla, a traves del Instituto de Bienestar
                  Animal, es responsable del tratamiento de tus datos personales.
                  Se usaran para integrar el Padron Estatal de Perros y Gatos,
                  emitir tu certificado y credencial, y comunicarte el resultado de
                  tu tramite.
                </Typography>
              </Alert>

              <FormControlLabel
                control={
                  <Checkbox
                    checked={aceptaAviso}
                    onChange={(e) => setAceptaAviso(e.target.checked)}
                  />
                }
                label={
                  ventanilla
                    ? 'La persona leyo y acepto el aviso de privacidad'
                    : 'He leido y acepto el aviso de privacidad'
                }
              />

              <Alert severity="warning" sx={{ mt: 2 }}>
                Los documentos (identificacion, comprobante de domicilio y
                fotografia de tu animal) los presentas al recoger tu certificado o
                el dia de la jornada.
              </Alert>
            </>
          )}

          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
            <Button
              onClick={() => setPaso((p) => p - 1)}
              disabled={paso === 0 || enviando}
              color="inherit"
            >
              Atras
            </Button>

            {paso < PASOS.length - 1 ? (
              <Button
                variant="contained"
                onClick={() => setPaso((p) => p + 1)}
                disabled={!pasoCompleto(paso)}
              >
                Continuar
              </Button>
            ) : (
              <Button
                variant="contained"
                size="large"
                onClick={enviar}
                disabled={!pasoCompleto(2) || enviando}
              >
                {enviando ? <CircularProgress size={22} color="inherit" /> : 'Enviar registro'}
              </Button>
            )}
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}

export default RegistroTramite;
