package org.zkoss.reporte.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.zkoss.reporte.security.user.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Inicializar roles si no existen
        inicializarRoles();

        // Crear usuario administrador por defecto si no existe
        crearUsuarioAdmin();
    }

    private void inicializarRoles() {
        // Definir roles predeterminados con sus permisos

        // Rol ADMIN con permiso de administración completa
        Set<Permission> permisosAdmin = new HashSet<>();
        permisosAdmin.add(Permission.ADMIN_ALL);
        crearRolSiNoExiste("ROLE_ADMIN", "Acceso completo a todas las funcionalidades", permisosAdmin);

        // Rol PROYECTOS con permisos CRUD para proyectos
        Set<Permission> permisosProyectos = new HashSet<>();
        permisosProyectos.add(Permission.PROYECTOS_READ);
        permisosProyectos.add(Permission.PROYECTOS_CREATE);
        permisosProyectos.add(Permission.PROYECTOS_UPDATE);
        permisosProyectos.add(Permission.PROYECTOS_DELETE);
        crearRolSiNoExiste("ROLE_PROYECTOS", "Gestión de proyectos", permisosProyectos);

        // Rol DATOS con permisos CRUD para datos
        Set<Permission> permisosDatos = new HashSet<>();
        permisosDatos.add(Permission.DATOS_READ);
        permisosDatos.add(Permission.DATOS_CREATE);
        permisosDatos.add(Permission.DATOS_UPDATE);
        permisosDatos.add(Permission.DATOS_DELETE);
        crearRolSiNoExiste("ROLE_DATOS", "Gestión de datos", permisosDatos);
    }

    private void crearRolSiNoExiste(String nombre, String descripcion, Set<Permission> permisos) {
        Optional<Rol> rolExistente = rolRepository.findByNombre(nombre);
        if (!rolExistente.isPresent()) {
            Rol rol = new Rol();
            rol.setNombre(nombre);
            rol.setDescripcion(descripcion);
            rol.setPermisos(permisos);
            rolRepository.save(rol);
        } else {
            // Actualizar permisos si el rol existe pero no tiene permisos
            Rol rol = rolExistente.get();
            if (rol.getPermisos() == null || rol.getPermisos().isEmpty()) {
                rol.setPermisos(permisos);
                rolRepository.save(rol);
            }
        }
    }

    private void crearUsuarioAdmin() {
        // Verificar si ya existe un usuario admin
        Optional<Usuario> usuarioExistente = usuarioRepository.findByUsername("admin");

        if (!usuarioExistente.isPresent()) {
            // Obtener rol admin
            Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

            // Crear conjunto de roles
            Set<Rol> roles = new HashSet<>();
            roles.add(rolAdmin);

            // Crear usuario admin
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin")); // Contraseña inicial
            admin.setEmail("admin@ejemplo.com");
            admin.setNombre("Administrador");
            admin.setApellido("Sistema");
            admin.setActivo(true);
            admin.setRoles(roles);

            usuarioRepository.save(admin);
        }
    }
}
