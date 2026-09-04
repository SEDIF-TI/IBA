package mx.gob.sedif.sea.core.animal;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.core.persona.PersonaRepository;
import mx.gob.sedif.sea.core.titularidad.Titularidad;
import mx.gob.sedif.sea.core.titularidad.TitularidadRepository;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

/**
 * Padron de animales.
 *
 * <p>Este es el padron propiamente dicho: lo que se empadrona es el animal. La
 * persona aparece como su tutor responsable, en una relacion con vigencia que
 * permite transferirlo sin perder la historia.
 */
@Service
public class AnimalService {

    private static final Logger log = LoggerFactory.getLogger(AnimalService.class);

    private final AnimalRepository animalRepository;
    private final PersonaRepository personaRepository;
    private final TitularidadRepository titularidadRepository;
    private final AuditoriaService auditoria;

    public AnimalService(AnimalRepository animalRepository,
                         PersonaRepository personaRepository,
                         TitularidadRepository titularidadRepository,
                         AuditoriaService auditoria) {
        this.animalRepository = animalRepository;
        this.personaRepository = personaRepository;
        this.titularidadRepository = titularidadRepository;
        this.auditoria = auditoria;
    }

    /**
     * Busca animales por nombre, clave interna o numero de chip.
     *
     * <p>El numero de chip sirve para el caso en que aparece un animal perdido:
     * se lee su chip y se localiza a su tutor.
     */
    @Transactional(readOnly = true)
    public Page<Animal> buscar(String texto, Pageable pageable) {
        return animalRepository.buscarPorNombre(texto, pageable);
    }

    @Transactional(readOnly = true)
    public Animal obtener(Long id) {
        return animalRepository.findConDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Animal", id));
    }

    /** Tutor responsable actual del animal. */
    @Transactional(readOnly = true)
    public Persona tutorVigente(Long animalId) {
        return titularidadRepository.findByAnimalIdAndVigenteTrue(animalId)
                .map(Titularidad::getPersona)
                .orElse(null);
    }

    /**
     * Historial de tutores del animal.
     *
     * <p>Se conserva completo: un animal transferido tuvo un tutor antes, y esa
     * historia importa para resolver dudas sobre a quien correspondia cuando.
     */
    @Transactional(readOnly = true)
    public List<Titularidad> historialTitularidad(Long animalId) {
        return titularidadRepository.findByAnimalIdOrderByFechaInicioDesc(animalId);
    }

    /**
     * Transfiere el animal a otro tutor.
     *
     * <p>Cierra la titularidad anterior y abre una nueva. No se sobrescribe la
     * anterior: la historia de quien respondio por el animal y hasta cuando es
     * parte del padron.
     *
     * @param animalId      animal que cambia de tutor
     * @param nuevoTutorId  tutor que lo recibe
     * @param motivo        por que se transfiere
     */
    @Transactional
    public Titularidad transferir(Long animalId, Long nuevoTutorId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException(
                    "Escribe el motivo de la transferencia (adopcion, cambio de "
                            + "responsable, fallecimiento del tutor). Queda en el "
                            + "historial del animal.");
        }

        Animal animal = obtener(animalId);

        Persona nuevoTutor = personaRepository.findById(nuevoTutorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor", nuevoTutorId));

        Titularidad vigente = titularidadRepository
                .findByAnimalIdAndVigenteTrue(animalId)
                .orElse(null);

        if (vigente != null) {
            if (vigente.getPersona().getId().equals(nuevoTutorId)) {
                throw new ReglaNegocioException(
                        "%s ya es el tutor responsable de %s".formatted(
                                nuevoTutor.getNombreCompleto(), animal.getNombre()));
            }
            vigente.cerrar(motivo);
            // Se escribe el cierre de inmediato: el indice unico impide dos
            // titularidades vigentes del mismo animal, asi que la nueva no
            // puede insertarse mientras la anterior siga marcada como vigente.
            titularidadRepository.saveAndFlush(vigente);
        }

        Titularidad nueva = new Titularidad();
        nueva.setAnimal(animal);
        nueva.setPersona(nuevoTutor);
        nueva.setFechaInicio(LocalDate.now());

        Titularidad guardada = titularidadRepository.saveAndFlush(nueva);

        // Se recarga con la persona cargada: el DTO se arma en el controlador,
        // fuera de esta transaccion.
        guardada = titularidadRepository.findByAnimalIdAndVigenteTrue(animalId)
                .orElse(guardada);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "titularidad", guardada.getId(),
                "%s pasa a ser tutor de %s: %s".formatted(
                        nuevoTutor.getNombreCompleto(), animal.getNombre(), motivo));

        log.info("Animal {} transferido al tutor {}", animalId, nuevoTutorId);
        return guardada;
    }

    /**
     * Registra la defuncion del animal.
     *
     * <p>El registro se conserva y su titularidad se cierra. Borrarlo perderia
     * la constancia de que estuvo empadronado, que puede necesitarse despues.
     */
    @Transactional
    public Animal registrarDefuncion(Long animalId, LocalDate fecha, String observacion) {
        Animal animal = obtener(animalId);

        if (Boolean.TRUE.equals(animal.getFinado())) {
            throw new ReglaNegocioException(
                    "La defuncion de %s ya estaba registrada".formatted(animal.getNombre()));
        }

        LocalDate fechaDefuncion = fecha != null ? fecha : LocalDate.now();

        if (fechaDefuncion.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La fecha de defuncion no puede ser posterior a hoy");
        }

        animal.setFinado(true);
        animal.setFechaDefuncion(fechaDefuncion);
        Animal guardado = animalRepository.saveAndFlush(animal);

        // La titularidad se cierra: ya no hay quien responda por el animal.
        titularidadRepository.findByAnimalIdAndVigenteTrue(animalId)
                .ifPresent(titularidad -> {
                    titularidad.cerrar("Defuncion del animal"
                            + (observacion != null && !observacion.isBlank()
                                    ? ": " + observacion : ""));
                    titularidadRepository.save(titularidad);
                });

        auditoria.registrar(AccionAuditoria.MODIFICAR, "animal", animalId,
                "Se registro la defuncion de %s el %s".formatted(
                        animal.getNombre(), fechaDefuncion));

        log.info("Defuncion registrada del animal {}", animalId);
        return animalRepository.findConDetallePorId(animalId).orElse(guardado);
    }
}
