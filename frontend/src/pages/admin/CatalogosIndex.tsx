import { Box, Card, CardActionArea, CardContent, Grid, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';

import { guinda } from '@/theme';

/** Catalogos que el personal puede administrar. */
const CATALOGOS = [
  { ruta: 'municipios', titulo: 'Municipios', detalle: 'Los 217 municipios de Puebla' },
  { ruta: 'especies', titulo: 'Especies', detalle: 'Canino y felino' },
  { ruta: 'razas', titulo: 'Razas', detalle: 'Razas por especie' },
  { ruta: 'colores', titulo: 'Colores', detalle: 'Colores del pelaje' },
  { ruta: 'sexos', titulo: 'Sexos', detalle: 'Macho y hembra' },
  { ruta: 'tamanos', titulo: 'Tamanos', detalle: 'Tallas del animal' },
  {
    ruta: 'metodos-identificacion',
    titulo: 'Identificacion',
    detalle: 'Microchip, placa, tatuaje',
  },
  {
    ruta: 'tipos-documento',
    titulo: 'Tipos de documento',
    detalle: 'Que se pide y en que formato',
  },
  {
    ruta: 'motivos-rechazo',
    titulo: 'Motivos de rechazo',
    detalle: 'Causas de observación o rechazo',
  },
  {
    ruta: 'estatus-solicitud',
    titulo: 'Estados del trámite',
    detalle: 'Nombre y color de cada estado',
  },
  { ruta: 'estatus-cita', titulo: 'Estados de la cita', detalle: 'Agendada, atendida' },
  { ruta: 'medios-contacto', titulo: 'Medios de contacto', detalle: 'Correo, telefono' },
  { ruta: 'sedes', titulo: 'Sedes de entrega', detalle: 'Donde se recoge la documentacion' },
];

/** Indice de catalogos administrables. */
export function CatalogosIndex() {
  const navegar = useNavigate();

  return (
    <Box>
      <Typography variant="h5" fontWeight={600} gutterBottom>
        Catálogos
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Valores que alimentan los formularios del sistema. Editarlos aquí evita
        que se capturen variantes del mismo concepto.
      </Typography>

      <Grid container spacing={2}>
        {CATALOGOS.map((catalogo) => (
          <Grid item xs={12} sm={6} md={4} key={catalogo.ruta}>
            <Card sx={{ height: '100%' }}>
              <CardActionArea
                onClick={() => navegar(`/admin/catalogos/${catalogo.ruta}`)}
                sx={{ height: '100%', p: 1 }}
              >
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={600} color={guinda[700]}>
                    {catalogo.titulo}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {catalogo.detalle}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

export default CatalogosIndex;
