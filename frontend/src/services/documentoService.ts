import api from './api';
import type { ApiResponse } from '@/types/api';
import type { Catalogo, Documento } from '@/types/dominio';

/**
 * Documentos adjuntos a un tramite.
 *
 * <p>La descarga no es una direccion directa al archivo: pasa por un destino
 * autenticado que valida el permiso y deja constancia de quien accedio. Los
 * archivos viven fuera del alcance de cualquier servidor web.
 */
export const documentoService = {
  async listarPorSolicitud(solicitudId: number): Promise<Documento[]> {
    const { data } = await api.get<ApiResponse<Documento[]>>(
      `/documentos/solicitud/${solicitudId}`,
    );
    return data.data;
  },

  /** Adjunta un archivo al tramite. */
  async adjuntar(
    solicitudId: number,
    tipoDocumentoId: number,
    archivo: File,
  ): Promise<Documento> {
    const formulario = new FormData();
    formulario.append('archivo', archivo);

    const { data } = await api.post<ApiResponse<Documento>>(
      `/documentos/solicitud/${solicitudId}`,
      formulario,
      {
        params: { tipoDocumentoId },
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    return data.data;
  },

  /**
   * Descarga un documento y lo entrega al navegador.
   *
   * <p>Cada descarga queda registrada en bitacora del lado del servidor.
   */
  async descargar(id: number, nombreArchivo: string): Promise<void> {
    const respuesta = await api.get<Blob>(`/documentos/${id}/descargar`, {
      responseType: 'blob',
    });

    const url = window.URL.createObjectURL(respuesta.data);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombreArchivo;
    document.body.appendChild(enlace);
    enlace.click();

    // Se libera la referencia para no dejar el archivo en memoria.
    document.body.removeChild(enlace);
    window.URL.revokeObjectURL(url);
  },

  /** Marca el documento como legible o ilegible. */
  async revisar(id: number, legible: boolean, observacion?: string): Promise<Documento> {
    const { data } = await api.patch<ApiResponse<Documento>>(`/documentos/${id}/revisar`, {
      legible,
      observacion,
    });
    return data.data;
  },

  async eliminar(id: number): Promise<void> {
    await api.delete(`/documentos/${id}`);
  },

  /**
   * Adjunta un documento desde el portal ciudadano, con el token del tramite.
   *
   * <p>No se manda el id de la solicitud: la persona no tiene sesion, y el
   * token es lo que demuestra que ese tramite es suyo.
   */
  async adjuntarPublico(
    token: string,
    tipoDocumentoId: number,
    archivo: File,
  ): Promise<Documento> {
    const formulario = new FormData();
    formulario.append('archivo', archivo);

    const { data } = await api.post<ApiResponse<Documento>>(
      '/publico/documento',
      formulario,
      {
        params: { token, tipoDocumentoId },
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    return data.data;
  },

  /** Tipos que la ciudadania puede adjuntar, sin sesion. */
  async tiposPublicos(): Promise<Catalogo[]> {
    const { data } = await api.get<ApiResponse<Catalogo[]>>('/publico/tipos-documento');
    return data.data;
  },

};

export default documentoService;