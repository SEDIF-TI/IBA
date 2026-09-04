package mx.gob.sedif.sea.core.persona;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.animal.AnimalRepository;
import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.MunicipioRepository;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.core.solicitud.SolicitudRepository;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

/**
 * Tutores responsables.
 *
 * <p>La persona no se empadrona: el padron es de animales. Aqui se consulta a
 * quien responde por ellos, sobre todo cuando alguien llega preguntando por sus
 * tramites o hay que saber cuantos animales tiene registrados.
 *
 * <p>Toda consulta por CURP queda en bitacora, porque es acceso a un dato
 * personal.
 */
@Service
public class PersonaService {

    private static final Logger log = LoggerFactory.getLogger(PersonaService.class);

    private final PersonaRepository personaRepository;
    private final AnimalRepository animalRepository;
    private final SolicitudRepository solicitudRepository;
    private final MunicipioRepository municipioRepository;
    private final AuditoriaService auditoria;

    public PersonaService(PersonaRepository personaRepository,
                          AnimalRepository animalRepository,
                          SolicitudRepository solicitudRepository,
                          MunicipioRepository municipioRepository,
                          AuditoriaService auditoria) {
        this.personaRepository = personaRepository;
        this.animalRepository = animalRepository;
        this.solicitudRepository = solicitudRepository;
        this.municipioRepository = municipioRepository;
        this.auditoria = auditoria;
    }

    /**
     * Busca tutores por nombre, apellidos o CURP.
     *
     * <p>Ignora acentos: "munoz" encuentra "Muñoz". Es como se teclea cuando hay
     * alguien esperando en la ventanilla.
     *
     * @param texto CURP completa, apellidos o nombre
     */
    @Transactional(readOnly = true)
    public Page<Persona> buscar(String texto, Pageable pageable) {
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();

        // Una CURP completa se busca exacta y queda en bitacora: es acceso
        // directo a los datos de una persona concreta.
        if (filtro != null && filtro.length() == 18) {
            String curp = CurpUtil.normalizar(filtro);
            Optional<Persona> encontrada = personaRepository.findByCurp(curp);

            auditoria.registrarConsultaCurp(curp,
                    encontrada.map(Persona::getId).orElse(null),
                    encontrada.isPresent());

            return encontrada
                    .map(persona -> (Page<Persona>) new org.springframework.data.domain.PageImpl<>(
                            List.of(persona), pageable, 1))
                    .orElseGet(() -> org.springframework.data.domain.Page.empty(pageable));
        }

        return personaRepository.buscarPorNombre(filtro, pageable);
    }

    /**
     * Ficha completa de un tutor.
     *
     * <p>Queda en bitacora: abrir la ficha es ver los datos personales de
     * alguien, y debe poder saberse quien lo hizo.
     */
    @Transactional(readOnly = true)
    public Persona obtener(Long id) {
        Persona persona = personaRepository.findConDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor", id));

        auditoria.registrar(AccionAuditoria.CONSULTAR_PERSONA, "persona", id,
                "Consulta de la ficha de " + persona.getNombreCompleto());

        return persona;
    }

    /**
     * Animales que el tutor tiene a su cargo ahora mismo.
     *
     * <p>Son las titularidades vigentes. Un animal transferido ya no cuenta para
     * el tutor anterior, aunque su historial se conserve.
     */
    @Transactional(readOnly = true)
    public List<Animal> animalesVigentes(Long personaId) {
        return animalRepository.findVigentesPorPersona(personaId);
    }

    /** Cuantos animales tiene a su cargo. */
    @Transactional(readOnly = true)
    public long contarAnimales(Long personaId) {
        return personaRepository.contarAnimalesVigentes(personaId);
    }

    /** Tramites del tutor, del mas reciente al mas antiguo. */
    @Transactional(readOnly = true)
    public Page<Solicitud> tramites(Long personaId, Pageable pageable) {
        return solicitudRepository.findByPersonaIdOrderByFechaRegistroDesc(personaId, pageable);
    }

    /**
     * Completa la CURP de un tutor que se registro sin ella.
     *
     * <p>Es el caso de quien llego sin su documento: se registro con su nombre y
     * el personal completa la CURP despues, al cotejar la identificacion.
     *
     * @param personaId tutor a completar
     * @param curp      CURP verificada contra el documento
     */
    @Transactional
    public Persona completarCurp(Long personaId, String curp) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor", personaId));

        if (persona.getCurp() != null && !persona.getCurp().isBlank()) {
            throw new ReglaNegocioException(
                    "Este tutor ya tiene CURP registrada. Si esta equivocada, "
                            + "corrigela desde la edicion de sus datos.");
        }

        String normalizada = CurpUtil.normalizar(curp);

        if (!CurpUtil.esValida(normalizada)) {
            throw new ReglaNegocioException(MessageConstants.CURP_INVALIDA);
        }

        if (personaRepository.existsByCurp(normalizada)) {
            throw new ReglaNegocioException(MessageConstants.CURP_DUPLICADA);
        }

        // Se avisa si las iniciales no cuadran, pero no se bloquea: hay
        // apellidos compuestos que la regla general no cubre.
        boolean coincide = CurpUtil.coincideConNombre(normalizada, persona.getNombre(),
                persona.getApellidoPaterno(), persona.getApellidoMaterno());

        persona.setCurp(normalizada);
        CurpUtil.extraerFechaNacimiento(normalizada).ifPresent(persona::setFechaNacimiento);
        CurpUtil.extraerSexo(normalizada).ifPresent(persona::setSexoCurp);
        CurpUtil.extraerEntidad(normalizada).ifPresent(persona::setEntidadNacimiento);

        // La identidad queda validada solo si el personal cotejo el documento,
        // que es justo lo que esta haciendo al completar este dato.
        persona.setIdentidadValidada(true);

        Persona guardada = personaRepository.saveAndFlush(persona);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "persona", personaId,
                "Se completo la CURP de %s%s".formatted(
                        persona.getNombreCompleto(),
                        coincide ? "" : " (las iniciales no corresponden con el nombre)"));

        log.info("CURP completada para el tutor {}{}", personaId,
                coincide ? "" : " con iniciales que no cuadran");

        return personaRepository.findConDetallePorId(personaId).orElse(guardada);
    }

    /** Actualiza los datos de contacto y domicilio del tutor. */
    @Transactional
    public Persona actualizar(Long personaId, ActualizarPersonaRequest peticion) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor", personaId));

        boolean tendraContacto = tieneValor(peticion.correo())
                || tieneValor(peticion.telefono())
                || tieneValor(peticion.telefonoWhatsapp());

        if (!tendraContacto) {
            throw new ReglaNegocioException(MessageConstants.CONTACTO_REQUERIDO);
        }

        persona.setCorreo(limpiar(peticion.correo()));
        persona.setTelefono(limpiar(peticion.telefono()));
        persona.setTelefonoWhatsapp(limpiar(peticion.telefonoWhatsapp()));
        persona.setCalle(limpiar(peticion.calle()));
        persona.setNumeroExterior(limpiar(peticion.numeroExterior()));
        persona.setNumeroInterior(limpiar(peticion.numeroInterior()));
        persona.setColonia(limpiar(peticion.colonia()));
        persona.setCodigoPostal(limpiar(peticion.codigoPostal()));

        if (peticion.municipioId() != null) {
            persona.setMunicipio(municipioRepository.findById(peticion.municipioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Municipio", peticion.municipioId())));
        }

        Persona guardada = personaRepository.saveAndFlush(persona);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "persona", personaId,
                "Se actualizaron los datos de " + persona.getNombreCompleto());

        return personaRepository.findConDetallePorId(personaId).orElse(guardada);
    }

    private boolean tieneValor(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String limpiar(String texto) {
        return tieneValor(texto) ? texto.trim() : null;
    }
}
