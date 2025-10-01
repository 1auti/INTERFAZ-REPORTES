package org.zkoss.dominial.security.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zkoss.dominial.security.user.*;
import org.zkoss.dominial.security.token.Token;
import org.zkoss.dominial.security.config.JwtService;
import org.zkoss.dominial.security.token.TokenService;

import java.util.*;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, TokenService tokenService, UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Autentica un usuario y genera un token JWT
     * @param request Solicitud de autenticación con username y password
     * @return Respuesta que contiene el token JWT
     */
    public AuthResponse authenticate(AuthRequest request) {
        logger.info("Intento de autenticación para el usuario: {}", request.getUsername());

        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Obtener los detalles del usuario autenticado
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Generar token JWT
            String jwtToken = jwtService.generateToken(userDetails);

            logger.info("Autenticación exitosa para el usuario: {}", request.getUsername());

            // Agregar el nombre de usuario a la respuesta
            return new AuthResponse(jwtToken, userDetails.getUsername());

        } catch (BadCredentialsException e) {
            logger.warn("Autenticación fallida para el usuario: {} - Credenciales incorrectas",
                    request.getUsername());
            throw e;
        } catch (Exception e) {
            logger.error("Error durante la autenticación del usuario: {}",
                    request.getUsername(), e);
            throw e;
        }
    }

    /**
     * Cierra la sesión del usuario revocando su token
     * @param token Token JWT a revocar
     * @return true si el token fue revocado, false en caso contrario
     */
    public boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Intento de logout con token nulo o vacío");
            return false;
        }

        String username = jwtService.extractUsername(token);
        logger.info("Procesando logout para el usuario: {}", username);

        Optional<Token> tokenEntity = tokenService.findByToken(token);
        if (tokenEntity.isPresent()) {
//            tokenService.marcarTokenComoRevocado(tokenEntity.get());
//            tokenService.marcarTokenComoExpirado(tokenEntity.get());
            logger.info("Token revocado exitosamente para el usuario: {}", username);
            return true;
        }

        logger.warn("Token no encontrado en la base de datos para el usuario: {}", username);
        return false;
    }

    /**
     * Revoca todos los tokens del usuario
     * @param username Nombre de usuario
     * @return true si la operación fue exitosa, false en caso contrario
     */
    public boolean revocarTodosLosTokens(String username) {
        logger.info("Revocando todos los tokens para el usuario: {}", username);

        Optional<Usuario> usuario = usuarioRepository.findByUsername(username);
        if (usuario.isPresent()) {
            tokenService.revocarTodosLosTokens(usuario.get());
            logger.info("Todos los tokens han sido revocados para el usuario: {}", username);
            return true;
        }

        logger.warn("No se encontró el usuario: {} para revocar los tokens", username);
        return false;
    }

    /**
     * Refresca un token generando uno nuevo
     * @param userDetails Detalles del usuario
     * @return Respuesta que contiene el nuevo token JWT
     */
    public AuthResponse refreshToken(UserDetails userDetails) {
        if (userDetails == null) {
            logger.error("Intento de refrescar token con userDetails nulo");
            throw new IllegalArgumentException("UserDetails no puede ser nulo");
        }

        logger.info("Refrescando token para el usuario: {}", userDetails.getUsername());
        String refreshedToken = jwtService.generateToken(userDetails);
        logger.info("Token refrescado exitosamente para el usuario: {}", userDetails.getUsername());

        return new AuthResponse(refreshedToken, userDetails.getUsername());
    }

    /**
     * Genera un nuevo token JWT para un usuario específico.
     * Este método se utiliza principalmente para generar tokens durante el proceso de refresh.
     *
     * @param usuario Entidad usuario para la cual generar el token
     * @return Token JWT generado
     */
    /**
     * Genera un nuevo token JWT para un usuario específico.
     * Este método convierte el usuario a un UserDetails antes de generar el token.
     *
     * @param usuario Entidad usuario para la cual generar el token
     * @return Token JWT generado
     */
    public String generateTokenForUser(Usuario usuario) {
        logger.info("Generando token para el usuario: {}", usuario.getUsername());

        // Lista para almacenar todas las autoridades
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Para cada rol, agregar las autoridades correspondientes
        for (Rol rol : usuario.getRoles()) {
            // Agregar el rol como autoridad
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().toUpperCase()));

            // Agregar los permisos asociados a este rol
            if (rol.getPermisos() != null) {
                for (Permission permiso : rol.getPermisos()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getPermission()));
                }
            }
        }

        UserDetails userDetails = new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isActivo(),
                true,
                true,
                true,
                authorities
        );

        // Generar el token JWT
        String token = jwtService.generateToken(userDetails);

        logger.info("Token generado exitosamente para el usuario: {}", usuario.getUsername());

        return token;
    }
    /**
     * Registra un nuevo usuario en el sistema
     *
     * @param request Datos del usuario a registrar
     * @return Respuesta con información de autenticación si el registro fue exitoso
     */
    @Transactional
    public AuthResponse registrarUsuario(RegistroRequest request) {
        logger.info("Iniciando registro de usuario: {}", request.getUsername());

        // Verificar si el nombre de usuario ya existe
        if (usuarioRepository.findByUsername(request.getUsername()).isPresent()) {
            logger.warn("Error en el registro: El nombre de usuario {} ya existe", request.getUsername());
            return AuthResponse.builder()
                    .message("El nombre de usuario ya está en uso")
                    .build();
        }

        // Verificar si el email ya existe
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Error en el registro: El email {} ya está registrado", request.getEmail());
            return AuthResponse.builder()
                    .message("El correo electrónico ya está registrado")
                    .build();
        }

        try {
            // Crear nueva entidad de usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setUsername(request.getUsername());
            nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
            nuevoUsuario.setEmail(request.getEmail());
            nuevoUsuario.setActivo(true);

            // Asignar rol de usuario básico
            Set<Rol> roles = new HashSet<>();

            // Determinar qué rol asignar
            String nombreRol = "ROLE_" + (request.getRolSolicitado() != null ?
                    request.getRolSolicitado().toUpperCase() : "PROYECTOS");

            // Buscar el rol
            Optional<Rol> rolSeleccionado = rolRepository.findByNombre(nombreRol);

            // Si no existe el rol solicitado, usar uno predeterminado
            if (!rolSeleccionado.isPresent()) {
                rolSeleccionado = rolRepository.findByNombre("ROLE_PROYECTOS");

                // Intentar con otros roles si el predeterminado no existe
                if (!rolSeleccionado.isPresent()) {
                    rolSeleccionado = rolRepository.findByNombre("ROLE_DATOS");
                }
                if (!rolSeleccionado.isPresent()) {
                    rolSeleccionado = rolRepository.findByNombre("ROLE_ADMIN");
                }
            }

            // Verificar si se encontró algún rol
            if (!rolSeleccionado.isPresent()) {
                throw new RuntimeException("No se encontró ningún rol para asignar al usuario");
            }

            // Comprobar si el rol tiene permisos asignados
            Rol rol = rolSeleccionado.get();
            if (rol.getPermisos() == null || rol.getPermisos().isEmpty()) {
                // Asignar permisos predeterminados según el tipo de rol
                Set<Permission> permisos = new HashSet<>();

                if (rol.getNombre().contains("ADMIN")) {
                    permisos.add(Permission.ADMIN_ALL);
                } else if (rol.getNombre().contains("PROYECTOS")) {
                    permisos.add(Permission.PROYECTOS_READ);
                    permisos.add(Permission.PROYECTOS_CREATE);
                    permisos.add(Permission.PROYECTOS_UPDATE);
                    permisos.add(Permission.PROYECTOS_DELETE);
                } else if (rol.getNombre().contains("DATOS")) {
                    permisos.add(Permission.DATOS_READ);
                    permisos.add(Permission.DATOS_CREATE);
                    permisos.add(Permission.DATOS_UPDATE);
                    permisos.add(Permission.DATOS_DELETE);
                }

                rol.setPermisos(permisos);
                rolRepository.save(rol);
            }

            roles.add(rol);
            nuevoUsuario.setRoles(roles);

            // Guardar el usuario
            Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);
            logger.info("Usuario registrado correctamente: {}", usuarioGuardado.getUsername());

            // Generar token JWT
            String token = generateTokenForUser(usuarioGuardado);

            // Crear respuesta exitosa
            return AuthResponse.builder()
                    .token(token)
                    .username(usuarioGuardado.getUsername())
                    .message("Usuario registrado correctamente")
                    .build();

        } catch (Exception e) {
            logger.error("Error durante el registro del usuario {}: {}",
                    request.getUsername(), e.getMessage(), e);

            return AuthResponse.builder()
                    .message("Error durante el registro: " + e.getMessage())
                    .build();
        }
    }


}