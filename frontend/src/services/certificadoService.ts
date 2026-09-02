import api from './api';
import type { ApiResponse } from '@/types/api';
import type { Certificado, Verificacion } from '@/types/dominio';

/**
 * Certificados de empadronamiento.
 *
 * <p>Los documentos se componen del lado del servidor al momento de pedirlos,
 * asi que aqui solo se reciben y se entregan al navegador.
 */
export const certificadoService = {
  async obtener(id: number): Promise<Certificado> {
    const { data } = await api.get<ApiResponse<Certificado>>(`/certificados/${id}`);
    return data.data;
  },

  /** Certificado de un tramite; nulo si aun no se emite. */
  async porSolicitud(solicitudId: number): Promise<Certificado | null> {
    const { data } = await api.get<ApiResponse<Certificado | null>>(
      `/certificados/solicitud/${solicitudId}`,
    );
    return data.data;
  },

  /** Emite el certificado de un tramite aprobado. */
  async emitir(solicitudId: number): Promise<Certificado> {
    const { data } = await api.post<ApiResponse<Certificado>>(
      `/certificados/solicitud/${solicitudId}`,
    );
    return data.data;
  },

  /** Descarga el certificado. */
  async descargarCertificado(id: number, folio: string): Promise<void> {
    await descargarPdf(`/certificados/${id}/pdf`, `certificado-${limpiar(folio)}.pdf`);
  },

  /** Descarga la credencial. */
  async descargarCredencial(id: number, folio: string): Promise<void> {
    await descargarPdf(`/certificados/${id}/credencial`, `credencial-${limpiar(folio)}.pdf`);
  },

  /**
   * Imprime varias credenciales de una vez.
   *
   * <p>Es lo que se usa al cierre de una jornada: se marcan los tramites y
   * salen tres por hoja, listas para recortar.
   */
  async imprimirLote(certificadoIds: number[]): Promise<void> {
    const respuesta = await api.post<Blob>(
      '/certificados/credenciales/lote',
      { certificadoIds },
      { responseType: 'blob' },
    );
    entregarAlNavegador(respuesta.data, 'credenciales-lote.pdf');
  },

  /** Cancela un certificado. */
  async cancelar(id: number, motivo: string): Promise<Certificado> {
    const { data } = await api.patch<ApiResponse<Certificado>>(
      `/certificados/${id}/cancelar`,
      { motivo },
    );
    return data.data;
  },

  /** Registra que el documento se entrego con su sello. */
  async registrarEntrega(id: number): Promise<Certificado> {
    const { data } = await api.patch<ApiResponse<Certificado>>(
      `/certificados/${id}/entregar`,
    );
    return data.data;
  },

  /**
   * Verifica un certificado por el codigo de su QR.
   *
   * <p>No requiere sesion: es el destino publico al que llega quien escanea el
   * documento.
   */
  async verificar(codigo: string): Promise<Verificacion> {
    const { data } = await api.get<ApiResponse<Verificacion>>(
      `/publico/verificar/${codigo}`,
    );
    return data.data;
  },
};

/** Pide un PDF al servidor y lo entrega al navegador. */
async function descargarPdf(ruta: string, nombreArchivo: string): Promise<void> {
  const respuesta = await api.get<Blob>(ruta, { responseType: 'blob' });
  entregarAlNavegador(respuesta.data, nombreArchivo);
}

/** Dispara la descarga en el navegador y libera la referencia. */
function entregarAlNavegador(contenido: Blob, nombreArchivo: string): void {
  const url = window.URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  document.body.removeChild(enlace);
  // Sin esto el archivo se queda en memoria del navegador.
  window.URL.revokeObjectURL(url);
}

/** Deja el folio utilizable como nombre de archivo. */
function limpiar(folio: string): string {
  return folio.replace(/[/\\:*?"<>|]/g, '-');
}

export default certificadoService;
