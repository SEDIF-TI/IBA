package mx.gob.sedif.sea.core.certificado;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Respuesta del verificador publico, destino del codigo QR.
 *
 * <p>Deliberadamente escueto: confirma que el certificado es autentico y
 * describe al animal, pero NO revela nada del tutor. Quien escanea el codigo
 * puede ser cualquiera (un veterinario, un vecino, quien encontro al animal
 * perdido), y no tiene por que enterarse del nombre, domicilio o telefono de
 * su responsable.
 *
 * @param valido            si el certificado existe y surte efecto
 * @param folio             folio oficial
 * @param animalNombre      nombre del animal
 * @param animalEspecie     especie
 * @param animalRaza        raza
 * @param animalSexo        sexo
 * @param animalColor       color
 * @param animalEdad        edad al momento de emitir
 * @param municipio         municipio de registro
 * @param fechaEmision      cuando se emitio
 * @param fechaVigencia     hasta cuando surte efecto
 * @param mensaje           explicacion para quien consulta
 */
public record VerificacionRecord(
        boolean valido,
        String folio,
        String animalNombre,
        String animalEspecie,
        String animalRaza,
        String animalSexo,
        String animalColor,
        String animalEdad,
        String municipio,
        OffsetDateTime fechaEmision,
        LocalDate fechaVigencia,
        String mensaje) {

    /** Certificado autentico y vigente. */
    public static VerificacionRecord valido(Certificado certificado) {
        var animal = certificado.getSolicitud().getAnimal();
        var persona = certificado.getSolicitud().getPersona();

        return new VerificacionRecord(
                true,
                certificado.getFolio(),
                animal.getNombre(),
                nombreDe(animal.getEspecie()),
                nombreDe(animal.getRaza()),
                nombreDe(animal.getSexo()),
                nombreDe(animal.getColor()),
                animal.getEdadDescriptiva(),
                persona.getMunicipio() == null ? null : persona.getMunicipio().getNombre(),
                certificado.getFechaEmision(),
                certificado.getFechaVigencia(),
                "Certificado autentico, expedido por el Instituto de Bienestar Animal.");
    }

    /**
     * Certificado que existe pero ya no surte efecto.
     *
     * <p>Se distingue del inexistente a proposito: saber que un certificado fue
     * cancelado o vencio es informacion util, y ocultarlo no protege a nadie.
     */
    public static VerificacionRecord noVigente(Certificado certificado, String motivo) {
        var animal = certificado.getSolicitud().getAnimal();

        return new VerificacionRecord(
                false,
                certificado.getFolio(),
                animal.getNombre(),
                nombreDe(animal.getEspecie()),
                null, null, null, null, null,
                certificado.getFechaEmision(),
                certificado.getFechaVigencia(),
                motivo);
    }

    /**
     * Codigo que no corresponde a ningun certificado.
     *
     * <p>No se devuelve dato alguno: probar codigos al azar no debe revelar
     * nada.
     */
    public static VerificacionRecord noEncontrado() {
        return new VerificacionRecord(
                false, null, null, null, null, null, null, null, null, null, null,
                "No existe ningun certificado con este codigo de verificacion.");
    }

    private static String nombreDe(mx.gob.sedif.sea.core.catalogo.CatalogoBase catalogo) {
        return catalogo == null ? null : catalogo.getNombre();
    }
}
