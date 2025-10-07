package org.zkoss.reporte.security.authentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.zkoss.reporte.security.user.Usuario;
import org.zkoss.reporte.security.user.UsuarioRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Lista para almacenar todas las autoridades (roles y permisos)
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Agregar roles como autoridades
        usuario.getRoles().forEach(rol -> {
            // Agregar el rol como una autoridad
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre().toUpperCase()));

            // Agregar cada permiso del rol como una autoridad adicional
            if (rol.getPermisos() != null) {
                rol.getPermisos().forEach(permiso ->
                        authorities.add(new SimpleGrantedAuthority(permiso.getPermission()))
                );
            }
        });

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.isActivo(),
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                authorities
        );
    }
}