package mx.gob.sedif.sea.core.animal;

import java.time.LocalDate;

/**
 * Animal empadronado, tal como se muestra en el padron.
 */
public record AnimalRecord(
        Long id,
        String nombre,
        String claveAnimal,
        String especie,
        String raza,
        String sexo,
        String color,
        String tamano,
        Integer edadMeses,
        String edadDescriptiva,
        Boolean vacunado,
        Boolean esterilizado,
        String caracteristicas,
        String metodoIdentificacion,
        String identificador,
        Boolean finado,
        LocalDate fechaDefuncion,
        String tutorNombre,
        Long tutorId) {

    public static AnimalRecord de(Animal animal) {
        return de(animal, null, null);
    }

    /**
     * @param tutorNombre tutor responsable actual; nulo si no se consulto
     * @param tutorId     identificador del tutor
     */
    public static AnimalRecord de(Animal animal, String tutorNombre, Long tutorId) {
        return new AnimalRecord(
                animal.getId(),
                animal.getNombre(),
                animal.getClaveAnimal(),
                nombreDe(animal.getEspecie()),
                nombreDe(animal.getRaza()),
                nombreDe(animal.getSexo()),
                nombreDe(animal.getColor()),
                nombreDe(animal.getTamano()),
                animal.getEdadMeses(),
                animal.getEdadDescriptiva(),
                animal.getVacunado(),
                animal.getEsterilizado(),
                animal.getCaracteristicas(),
                nombreDe(animal.getMetodoIdentificacion()),
                animal.getIdentificador(),
                animal.getFinado(),
                animal.getFechaDefuncion(),
                tutorNombre,
                tutorId);
    }

    private static String nombreDe(mx.gob.sedif.sea.core.catalogo.CatalogoBase catalogo) {
        return catalogo == null ? null : catalogo.getNombre();
    }
}
