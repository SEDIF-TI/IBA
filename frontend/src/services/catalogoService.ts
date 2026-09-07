import api from './api';
import type { ApiResponse, Pagina, ParametrosPagina } from '@/types/api';
import type { Catalogo, CatalogoRequest, Municipio } from '@/types/dominio';

/**
 * Acceso a los catalogos administrables.
 *
 * <p>Los trece se comportan igual, asi que el servicio se arma indicando la
 * ruta. Asi una pantalla sirve para todos y no hay trece servicios calcados.
 */
export function crearCatalogoService<T extends Catalogo = Catalogo>(ruta: string) {
  const base = `/catalogos/${ruta}`;

  return {
    /** Listado paginado para la pantalla de administracion. */
    async listar(
      parametros: ParametrosPagina & { texto?: string; soloActivos?: boolean } = {},
    ): Promise<Pagina<T>> {
      const { data } = await api.get<ApiResponse<Pagina<T>>>(base, { params: parametros });
      return data.data;
    },

    /** Opciones vigentes para los desplegables de los formularios. */
    async opciones(): Promise<T[]> {
      const { data } = await api.get<ApiResponse<T[]>>(`${base}/opciones`);
      return data.data;
    },

    async obtener(id: number): Promise<T> {
      const { data } = await api.get<ApiResponse<T>>(`${base}/${id}`);
      return data.data;
    },

    async crear(peticion: CatalogoRequest): Promise<T> {
      const { data } = await api.post<ApiResponse<T>>(base, peticion);
      return data.data;
    },

    async actualizar(id: number, peticion: CatalogoRequest): Promise<T> {
      const { data } = await api.put<ApiResponse<T>>(`${base}/${id}`, peticion);
      return data.data;
    },

    /** Baja logica: deja de ofrecerse, pero lo capturado sigue siendo legible. */
    async desactivar(id: number): Promise<void> {
      await api.delete(`${base}/${id}`);
    },

    /** Vuelve a poner disponible un valor dado de baja. */
    async reactivar(id: number): Promise<T> {
      const { data } = await api.patch<ApiResponse<T>>(`${base}/${id}/reactivar`);
      return data.data;
    },
  };
}

/** Los trece catalogos del sistema. */
export const catalogos = {
  municipios: crearCatalogoService('municipios'),
  especies: crearCatalogoService('especies'),
  razas: crearCatalogoService('razas'),
  colores: crearCatalogoService('colores'),
  sexos: crearCatalogoService('sexos'),
  tamanos: crearCatalogoService('tamanos'),
  metodosIdentificacion: crearCatalogoService('metodos-identificacion'),
  tiposDocumento: crearCatalogoService('tipos-documento'),
  motivosRechazo: crearCatalogoService('motivos-rechazo'),
  estatusSolicitud: crearCatalogoService('estatus-solicitud'),
  estatusCita: crearCatalogoService('estatus-cita'),
  mediosContacto: crearCatalogoService('medios-contacto'),
  sedes: crearCatalogoService('sedes'),
};

export default catalogos;

/**
 * Catalogos para el formulario publico, en una sola llamada y sin sesion.
 *
 * <p>El portal ciudadano no tiene usuario: si pidiera /catalogos, cada
 * desplegable respondaria 401 y la persona terminaria en la pantalla de acceso
 * del personal sin haber hecho nada malo.
 */
export async function catalogosPublicos(): Promise<CatalogosPublicos> {
  const { data } = await api.get<ApiResponse<CatalogosPublicos>>('/publico/catalogos');
  return data.data;
}

export interface CatalogosPublicos {
  municipios: Municipio[];
  especies: Catalogo[];
  colores: Catalogo[];
  sexos: Catalogo[];
  tamanos: Catalogo[];
}
