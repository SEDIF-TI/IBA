package mx.gob.sedif.sea.core.solicitud;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un tramite desde el portal, sin cuenta de usuario.
 *
 * <p>Llega en un solo envio con los datos del tutor, los del animal y la
 * aceptacion del aviso de privacidad. No hay registro previo: a la persona solo
 * le interesa su documentacion, no tener una cuenta mas.
 *
 * @param tutor            datos de la persona responsable
 * @param animal           datos del animal
 * @param aceptaAviso      aceptacion del aviso de privacidad
 * @param avisoVersion     version del aviso que se le mostro
 */
public record RegistroPublicoRequest(
        @NotNull(message = "Los datos del tutor son obligatorios")
        @Valid
        TutorRequest tutor,

        @NotNull(message = "Los datos del animal son obligatorios")
        @Valid
        AnimalRequest animal,

        @AssertTrue(message = "Debes aceptar el aviso de privacidad para continuar")
        @NotNull
        Boolean aceptaAviso,

        @NotBlank(message = "Falta la version del aviso de privacidad")
        String avisoVersion) {

    /**
     * Datos del tutor responsable.
     *
     * <p>La CURP es deseable pero no obligatoria: si la persona no la trae, se
     * registra con su nombre y el personal la completa despues a partir del INE.
     *
     * @param curp             CURP, opcional
     * @param nombre           nombre de pila
     * @param apellidoPaterno  primer apellido
     * @param apellidoMaterno  segundo apellido, opcional
     * @param correo           correo, opcional si deja telefono
     * @param telefono         telefono, opcional si deja correo
     * @param telefonoWhatsapp numero de WhatsApp, opcional
     * @param municipioId      municipio de domicilio
     * @param calle            calle
     * @param numeroExterior   numero exterior
     * @param numeroInterior   numero interior, opcional
     * @param colonia          colonia
     * @param codigoPostal     codigo postal
     */
    public record TutorRequest(
            @Pattern(regexp = "^$|^[A-Za-z]{4}[0-9]{6}[HMhm][A-Za-z]{5}[0-9A-Za-z][0-9]$",
                     message = "La CURP no tiene el formato correcto")
            String curp,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 120)
            String nombre,

            @NotBlank(message = "El apellido paterno es obligatorio")
            @Size(max = 120)
            String apellidoPaterno,

            @Size(max = 120)
            String apellidoMaterno,

            @Email(message = "El correo no es valido")
            @Size(max = 150)
            String correo,

            @Pattern(regexp = "^$|^[0-9]{10}$",
                     message = "El telefono debe tener 10 digitos")
            String telefono,

            @Pattern(regexp = "^$|^[0-9]{10}$",
                     message = "El numero de WhatsApp debe tener 10 digitos")
            String telefonoWhatsapp,

            @NotNull(message = "El municipio es obligatorio")
            Integer municipioId,

            @Size(max = 200) String calle,
            @Size(max = 20) String numeroExterior,
            @Size(max = 20) String numeroInterior,
            @Size(max = 150) String colonia,

            @Pattern(regexp = "^$|^[0-9]{5}$",
                     message = "El codigo postal debe tener 5 digitos")
            String codigoPostal) {

        /**
         * Comprueba que haya al menos una via de contacto.
         *
         * <p>Sin correo ni telefono no hay forma de avisar el resultado, y el
         * tramite se convierte en un callejon sin salida.
         */
        @AssertTrue(message = "Debes proporcionar al menos un correo o un telefono")
        public boolean isContactoPresente() {
            return (correo != null && !correo.isBlank())
                    || (telefono != null && !telefono.isBlank())
                    || (telefonoWhatsapp != null && !telefonoWhatsapp.isBlank());
        }
    }

    /**
     * Datos del animal.
     *
     * @param nombre                 nombre del animal
     * @param especieId              especie
     * @param razaId                 raza, opcional
     * @param colorId                color, opcional
     * @param sexoId                 sexo
     * @param tamanoId               talla, opcional
     * @param edadAnios              anos cumplidos
     * @param edadMeses              meses adicionales
     * @param vacunado               si esta vacunado
     * @param esterilizado           si esta esterilizado
     * @param caracteristicas        senas particulares
     * @param metodoIdentificacionId como esta identificado, opcional
     * @param identificador          numero de chip o placa, opcional
     */
    public record AnimalRequest(
            @NotBlank(message = "El nombre del animal es obligatorio")
            @Size(max = 120)
            String nombre,

            @NotNull(message = "La especie es obligatoria")
            Integer especieId,

            Integer razaId,
            Integer colorId,

            @NotNull(message = "El sexo es obligatorio")
            Integer sexoId,

            Integer tamanoId,

            @Min(value = 0, message = "Los anos no pueden ser negativos")
            @Max(value = 50, message = "Revisa la edad capturada")
            Integer edadAnios,

            @Min(value = 0, message = "Los meses no pueden ser negativos")
            @Max(value = 11, message = "Los meses deben ir de 0 a 11")
            Integer edadMeses,

            Boolean vacunado,
            Boolean esterilizado,

            @Size(max = 1000)
            String caracteristicas,

            Integer metodoIdentificacionId,

            @Size(max = 60)
            String identificador) {

        /**
         * Edad total en meses.
         *
         * <p>Se guarda normalizada: el sistema anterior la tenia como texto
         * libre ("1", "1 ANO", "2 MESES") y asi no se podia ordenar ni sacar
         * estadistica.
         */
        public Integer edadTotalMeses() {
            if (edadAnios == null && edadMeses == null) {
                return null;
            }
            return (edadAnios == null ? 0 : edadAnios) * 12
                    + (edadMeses == null ? 0 : edadMeses);
        }
    }
}
