import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import CargaPagina from '@/components/CargaPagina';

import ProtectedRoute from '@/components/ProtectedRoute';
import AdminLayout from '@/layouts/AdminLayout';
import PublicLayout from '@/layouts/PublicLayout';

const AdminCatalogo = lazy(() => import('@/pages/admin/AdminCatalogo'));
const AdminJornadas = lazy(() => import('@/pages/admin/AdminJornadas'));
const BandejaContacto = lazy(() => import('@/pages/admin/BandejaContacto'));
const BandejaSolicitudes = lazy(() => import('@/pages/admin/BandejaSolicitudes'));
const CapturaVentanilla = lazy(() => import('@/pages/admin/CapturaVentanilla'));
const OperarJornada = lazy(() => import('@/pages/admin/OperarJornada'));
const DetalleSolicitud = lazy(() => import('@/pages/admin/DetalleSolicitud'));
const FichaAnimal = lazy(() => import('@/pages/admin/FichaAnimal'));
const FichaTutor = lazy(() => import('@/pages/admin/FichaTutor'));
const PadronAnimales = lazy(() => import('@/pages/admin/PadronAnimales'));
const PadronTutores = lazy(() => import('@/pages/admin/PadronTutores'));
const RegistrosDadosDeBaja = lazy(() => import('@/pages/admin/RegistrosDadosDeBaja'));
const CambiarPassword = lazy(() => import('@/pages/admin/CambiarPassword'));
const AdminUsuarios = lazy(() => import('@/pages/admin/AdminUsuarios'));
const CatalogosIndex = lazy(() => import('@/pages/admin/CatalogosIndex'));
const Login = lazy(() => import('@/pages/admin/Login'));
const SinPermiso = lazy(() => import('@/pages/admin/SinPermiso'));
const Tablero = lazy(() => import('@/pages/admin/Tablero'));

const CalendarioJornadas = lazy(() => import('@/pages/publico/CalendarioJornadas'));
const ConsultaTramite = lazy(() => import('@/pages/publico/ConsultaTramite'));
const Inicio = lazy(() => import('@/pages/publico/Inicio'));
const RegistroTramite = lazy(() => import('@/pages/publico/RegistroTramite'));
const VerificarCertificado = lazy(() => import('@/pages/publico/VerificarCertificado'));

/**
 * Rutas del sistema.
 *
 * <p>Dos mundos separados: el portal ciudadano, sin sesion, y el panel
 * administrativo, que exige entrar. La separacion es a proposito: la ciudadania
 * no tiene cuenta y no deberia toparse nunca con una pantalla de acceso.
 *
 * <p>Cada pantalla se carga de forma diferida (React.lazy): su codigo se descarga
 * al visitarla. Los layouts envuelven su Outlet en Suspense para que el
 * encabezado y el menu no desaparezcan mientras tanto; este Suspense exterior
 * cubre las pantallas que van fuera de un layout (acceso y cambio de contrasena).
 */
export function App() {
  return (
    <Suspense fallback={<CargaPagina />}>
      <Routes>
        {/* ---- Portal ciudadano ---- */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/registro" element={<RegistroTramite />} />
          <Route path="/consulta" element={<ConsultaTramite />} />
          {/* Destino del codigo QR impreso en el certificado. La variante con
              codigo verifica sola; la otra permite capturarlo a mano. */}
          <Route path="/verificar" element={<VerificarCertificado />} />
          <Route path="/verificar/:codigo" element={<VerificarCertificado />} />
          <Route path="/jornadas" element={<CalendarioJornadas />} />
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
            path="solicitudes"
            element={
              <ProtectedRoute permiso="solicitud:ver">
                <BandejaSolicitudes />
              </ProtectedRoute>
            }
          />
          <Route
            path="solicitudes/nueva"
            element={
              <ProtectedRoute permiso="solicitud:crear">
                <CapturaVentanilla />
              </ProtectedRoute>
            }
          />
          <Route
            path="solicitudes/dados-de-baja"
            element={
              <ProtectedRoute permiso="solicitud:depurar">
                <RegistrosDadosDeBaja />
              </ProtectedRoute>
            }
          />
          <Route
            path="solicitudes/:id"
            element={
              <ProtectedRoute permiso="solicitud:ver">
                <DetalleSolicitud />
              </ProtectedRoute>
            }
          />

          <Route
            path="padron"
            element={
              <ProtectedRoute permiso="animal:ver">
                <PadronAnimales />
              </ProtectedRoute>
            }
          />
          <Route
            path="padron/:id"
            element={
              <ProtectedRoute permiso="animal:ver">
                <FichaAnimal />
              </ProtectedRoute>
            }
          />

          <Route
            path="tutores"
            element={
              <ProtectedRoute permiso="persona:ver">
                <PadronTutores />
              </ProtectedRoute>
            }
          />
          <Route
            path="tutores/:id"
            element={
              <ProtectedRoute permiso="persona:ver">
                <FichaTutor />
              </ProtectedRoute>
            }
          />

          <Route
            path="jornadas"
            element={
              <ProtectedRoute permiso="jornada:ver">
                <AdminJornadas />
              </ProtectedRoute>
            }
          />
          <Route
            path="jornadas/:id/operar"
            element={
              <ProtectedRoute permiso="jornada:operar">
                <OperarJornada />
              </ProtectedRoute>
            }
          />

          <Route
            path="contacto"
            element={
              <ProtectedRoute permiso="notificacion:contactar">
                <BandejaContacto />
              </ProtectedRoute>
            }
          />

          <Route
            path="catalogos"
            element={
              <ProtectedRoute permiso="catalogo:ver">
                <CatalogosIndex />
              </ProtectedRoute>
            }
          />

          <Route
            path="usuarios"
            element={
              <ProtectedRoute permiso="usuario:ver">
                <AdminUsuarios />
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
                titulo="Metodos de identificación"
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
                titulo="Estados del trámite"
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
    </Suspense>
  );
}

export default App;
