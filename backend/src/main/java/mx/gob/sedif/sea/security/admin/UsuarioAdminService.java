package mx.gob.sedif.sea.security.admin;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.Sede;
import mx.gob.sedif.sea.core.catalogo.SedeRepository;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.Rol;
import mx.gob.sedif.sea.security.RolRepository;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Alta y mantenimiento de las cuentas del personal.
 *
 * <p>No hay autoservicio: las cuentas las crea informatica. Quien atiende
 * ventanilla no se registra solo, porque su cuenta decide sobre tramites de
 * terceros y debe existir alguien que responda por haberla creado.
 *
 * <p>La contrasena inicial se genera aqui y se entrega una sola vez a quien
 * dio de alta la cuenta. Nunca se guarda en claro ni se puede volver a ver:
 * si se pierde, se genera otra.
 */
@Service
public class UsuarioAdminService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioAdminService.class);

    /** Sin caracteres ambiguos: 0/O y 1/l/I se confunden al dictar por telefono. */
    private static final String ALFABETO =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LARGO_TEMPORAL = 12;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder codificador;
    private final AuditoriaService auditoria;
    private final SecureRandom azar = new SecureRandom();

    public UsuarioAdminService(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               SedeRepository sedeRepository,
                               PasswordEncoder codificador,
                               AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.sedeRepository = sedeRepository;
        this.codificador = codificador;
        this.auditoria = auditoria;
    }

    /** Listado del personal, con o sin las cuentas dadas de baja. */
    @Transactional(readOnly = true)
    public Page<Usuario> buscar(String texto, Boolean soloActivos, Pageable pageable) {
        return usuarioRepository.buscar(
                texto == null || texto.isBlank() ? "" : texto.trim(),
                soloActivos == null || soloActivos,
                pageable);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findConRolesPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    /**
     * Da de alta una cuenta y devuelve su contrasena temporal.
     *
     * <p>La cuenta nace obligada a cambiarla en el primer acceso: asi la
     * contrasena que se dicto por telefono deja de servir en cuanto la persona
     * entra, y nadie mas conserva una valida.
     */
    @Transactional
    public ResultadoAlta crear(GuardarUsuarioRequest peticion) {
        String usuario = peticion.usuario().trim().toLowerCase();
        String correo = peticion.correo().trim().toLowerCase();

        if (usuarioRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new ReglaNegocioException(
                    "Ya existe una cuenta con el usuario '%s'. Elige otro nombre de usuario."
                            .formatted(usuario));
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException(
                    "Ya hay una cuenta registrada con el correo '%s'. ".formatted(correo)
                            + "Si la persona ya tiene cuenta, reactivala en vez de crear otra.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsuario(usuario);
        nuevo.setCorreo(correo);
        aplicarDatos(nuevo, peticion);

        String temporal = generarPasswordTemporal();
        nuevo.setPasswordHash(codificador.encode(temporal));
        nuevo.setCambioPasswordRequerido(Boolean.TRUE);
        nuevo.setActivo(Boolean.TRUE);
        nuevo.setIntentosFallidos(0);

        Usuario guardado = usuarioRepository.saveAndFlush(nuevo);

        auditoria.registrar(AccionAuditoria.CREAR, "usuario", guardado.getId(),
                "Alta de la cuenta '%s' (%s) con los roles: %s".formatted(
                        guardado.getUsuario(),
                        guardado.getNombreCompleto(),
                        nombresDeRoles(guardado)));

        log.info("Cuenta '{}' creada por '{}'", guardado.getUsuario(),
                SecurityUtil.obtenerNombreUsuario().orElse("sistema"));

        return new ResultadoAlta(guardado, temporal);
    }

    /** Actualiza los datos y los roles de una cuenta. */
    @Transactional
    public Usuario actualizar(Long id, GuardarUsuarioRequest peticion) {
        Usuario usuario = obtener(id);

        String correo = peticion.correo().trim().toLowerCase();
        usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException(
                            "El correo '%s' ya lo usa la cuenta '%s'.".formatted(
                                    correo, otro.getUsuario()));
                });

        usuario.setCorreo(correo);
        aplicarDatos(usuario, peticion);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "usuario", guardado.getId(),
                "Cambio de datos de la cuenta '%s'. Roles: %s".formatted(
                        guardado.getUsuario(), nombresDeRoles(guardado)));

        return guardado;
    }

    /**
     * Reinicia la contrasena y devuelve la nueva temporal.
     *
     * <p>Es el camino cuando alguien la olvida: no se puede recuperar la
     * anterior porque no se guarda, solo su huella.
     */
    @Transactional
    public ResultadoAlta reiniciarPassword(Long id) {
        Usuario usuario = obtener(id);

        String temporal = generarPasswordTemporal();
        usuario.setPasswordHash(codificador.encode(temporal));
        usuario.setCambioPasswordRequerido(Boolean.TRUE);
        // Se libera el bloqueo: la contrasena vieja ya no sirve.
        usuario.setIntentosFallidos(0);
        usuario.setBloqueoHasta(null);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        auditoria.registrar(AccionAuditoria.CAMBIO_PASSWORD, "usuario", guardado.getId(),
                "Se reinicio la contrasena de la cuenta '%s'".formatted(guardado.getUsuario()));

        return new ResultadoAlta(guardado, temporal);
    }

    /**
     * Da de baja o reactiva una cuenta.
     *
     * <p>Es baja logica: la cuenta deja de entrar pero se conserva, porque su
     * nombre aparece en los tramites que resolvio y el expediente debe seguir
     * diciendo quien los atendio.
     */
    @Transactional
    public Usuario cambiarVigencia(Long id, boolean activa) {
        Usuario usuario = obtener(id);

        String propio = SecurityUtil.obtenerNombreUsuario().orElse("");
        if (!activa && usuario.getUsuario().equalsIgnoreCase(propio)) {
            throw new ReglaNegocioException(
                    "No puedes dar de baja tu propia cuenta: quedarias fuera del sistema. "
                    + "Pide a otra persona con permiso que lo haga.");
        }

        if (!activa && esUltimoSuperadministrador(usuario)) {
            throw new ReglaNegocioException(
                    "Es la unica cuenta de superadministrador activa. Si se da de baja, "
                    + "nadie podria volver a crear cuentas ni asignar permisos. "
                    + "Crea otra antes de dar de baja esta.");
        }

        usuario.setActivo(activa);
        if (activa) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueoHasta(null);
        }

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "usuario", guardado.getId(),
                (activa ? "Reactivacion" : "Baja") + " de la cuenta '"
                        + guardado.getUsuario() + "'");

        return guardado;
    }

    /** Desbloquea una cuenta que agoto sus intentos de acceso. */
    @Transactional
    public Usuario desbloquear(Long id) {
        Usuario usuario = obtener(id);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueoHasta(null);

        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        auditoria.registrar(AccionAuditoria.MODIFICAR, "usuario", guardado.getId(),
                "Se desbloqueo la cuenta '%s'".formatted(guardado.getUsuario()));
        return guardado;
    }

    /** Roles que se pueden asignar. */
    @Transactional(readOnly = true)
    public List<Rol> rolesDisponibles() {
        return rolRepository.findByActivoTrueOrderByNombreAsc();
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private void aplicarDatos(Usuario usuario, GuardarUsuarioRequest peticion) {
        usuario.setNombre(peticion.nombre().trim());
        usuario.setApellidoPaterno(peticion.apellidoPaterno().trim());
        usuario.setApellidoMaterno(
                peticion.apellidoMaterno() == null || peticion.apellidoMaterno().isBlank()
                        ? null : peticion.apellidoMaterno().trim());
        usuario.setTelefono(
                peticion.telefono() == null || peticion.telefono().isBlank()
                        ? null : peticion.telefono().trim());

        if (peticion.sedeId() != null) {
            Sede sede = sedeRepository.findById(peticion.sedeId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sede", peticion.sedeId()));
            usuario.setSede(sede);
        } else {
            usuario.setSede(null);
        }

        if (peticion.rolesIds() == null || peticion.rolesIds().isEmpty()) {
            throw new ReglaNegocioException(
                    "Asigna al menos un rol: sin rol, la cuenta puede entrar pero no "
                    + "puede hacer nada dentro del sistema.");
        }

        Set<Rol> roles = new HashSet<>();
        for (Integer rolId : peticion.rolesIds()) {
            roles.add(rolRepository.findById(rolId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol", rolId)));
        }
        usuario.setRoles(roles);
    }

    /** Comprueba que no se quede el sistema sin quien administre. */
    private boolean esUltimoSuperadministrador(Usuario usuario) {
        boolean loEs = usuario.getRoles().stream()
                .anyMatch(rol -> "SUPERADMINISTRADOR".equals(rol.getClave()));
        if (!loEs) {
            return false;
        }
        return usuarioRepository.contarActivosPorRol("SUPERADMINISTRADOR") <= 1;
    }

    private String nombresDeRoles(Usuario usuario) {
        return usuario.getRoles().stream().map(Rol::getNombre).sorted().reduce(
                (a, b) -> a + ", " + b).orElse("sin rol");
    }

    private String generarPasswordTemporal() {
        StringBuilder sb = new StringBuilder(LARGO_TEMPORAL);
        for (int i = 0; i < LARGO_TEMPORAL; i++) {
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Cuenta creada junto con la contrasena que hay que entregarle. */
    public record ResultadoAlta(Usuario usuario, String passwordTemporal) {
    }
}
