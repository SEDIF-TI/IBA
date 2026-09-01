import { Navigate, Route, Routes } from 'react-router-dom';

import ProtectedRoute from '@/components/ProtectedRoute';
import AdminLayout from '@/layouts/AdminLayout';
import PublicLayout from '@/layouts/PublicLayout';

import AdminCatalogo from '@/pages/admin/AdminCatalogo';
import CambiarPassword from '@/pages/admin/CambiarPassword';
import CatalogosIndex from '@/pages/admin/CatalogosIndex';
import Login from '@/pages/admin/Login';
import SinPermiso from '@/pages/admin/SinPermiso';
import Tablero from '@/pages/admin/Tablero';

import ConsultaTramite from '@/pages/publico/ConsultaTramite';
import Inicio from '@/pages/publico/Inicio';

/**
 * Rutas del sistema.
 *
 * <p>Dos mundos separados: el portal ciudadano, sin sesion, y el panel
 * administrativo, que exige entrar. La separacion es a proposito: la ciudadania
 * no tiene cuenta y no deberia toparse nunca con una pantalla de acceso.
 */
export function App() {
  return (
    <Routes>
      {/* ---- Portal ciudadano ---- */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Inicio />} />
        <Route path="/consulta" element={<ConsultaTramite />} />
      </Route>

      {/* ---- Acceso al panel ---- */}
      <Route path="/admin/login" element={<Login />} />

      {/* Fuera del layout: quien tiene la contrasena por cambiar todavia no
          deberia ver el menu ni poder navegar a otra parte. */}
      <Route
        path="/admin/cambiar-password"
        element={
          <ProtectedRoute>
            <CambiarPassword />
          </ProtectedRoute>
        }
      />

      {/* ---- Panel administrativo ---- */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Tablero />} />
        <Route path="sin-permiso" element={<SinPermiso />} />

        <Route
          path="catalogos"
          element={
            <ProtectedRoute permiso="catalogo:ver">
              <CatalogosIndex />
            </ProtectedRoute>
          }
        />

        {/* Una sola pantalla atiende los trece catalogos. */}
        <Route
          path="catalogos/municipios"
          element={<AdminCatalogo ruta="municipios" titulo="Municipios" singular="Municipio" />}
        />
        <Route
          path="catalogos/especies"
          element={<AdminCatalogo ruta="especies" titulo="Especies" singular="Especie" />}
        />
        <Route
          path="catalogos/razas"
          element={<AdminCatalogo ruta="razas" titulo="Razas" singular="Raza" />}
        />
        <Route
          path="catalogos/colores"
          element={<AdminCatalogo ruta="colores" titulo="Colores" singular="Color" />}
        />
        <Route
          path="catalogos/sexos"
          element={<AdminCatalogo ruta="sexos" titulo="Sexos" singular="Sexo" />}
        />
        <Route
          path="catalogos/tamanos"
          element={<AdminCatalogo ruta="tamanos" titulo="Tamanos" singular="Tamano" />}
        />
        <Route
          path="catalogos/metodos-identificacion"
          element={
            <AdminCatalogo
              ruta="metodos-identificacion"
              titulo="Metodos de identificacion"
              singular="Metodo"
            />
          }
        />
        <Route
          path="catalogos/tipos-documento"
          element={
            <AdminCatalogo
              ruta="tipos-documento"
              titulo="Tipos de documento"
              singular="Tipo de documento"
            />
          }
        />
        <Route
          path="catalogos/motivos-rechazo"
          element={
            <AdminCatalogo
              ruta="motivos-rechazo"
              titulo="Motivos de rechazo"
              singular="Motivo"
            />
          }
        />
        <Route
          path="catalogos/estatus-solicitud"
          element={
            <AdminCatalogo
              ruta="estatus-solicitud"
              titulo="Estados del tramite"
              singular="Estado"
            />
          }
        />
        <Route
          path="catalogos/estatus-cita"
          element={
            <AdminCatalogo ruta="estatus-cita" titulo="Estados de la cita" singular="Estado" />
          }
        />
        <Route
          path="catalogos/medios-contacto"
          element={
            <AdminCatalogo
              ruta="medios-contacto"
              titulo="Medios de contacto"
              singular="Medio de contacto"
            />
          }
        />
        <Route
          path="catalogos/sedes"
          element={<AdminCatalogo ruta="sedes" titulo="Sedes de entrega" singular="Sede" />}
        />
      </Route>

      {/* Cualquier otra direccion regresa a la portada. */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
