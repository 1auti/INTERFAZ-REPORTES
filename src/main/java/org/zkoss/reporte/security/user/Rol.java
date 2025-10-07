package org.zkoss.reporte.security.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    // Relación many-to-many con permisos
    @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "roles_permisos",
            joinColumns = @JoinColumn(name = "rol_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso")
    private Set<Permission> permisos = new HashSet<>();

    // Método utilitario para agregar permisos
    public void agregarPermiso(Permission permiso) {
        if (permisos == null) {
            permisos = new HashSet<>();
        }
        permisos.add(permiso);
    }

    // Método utilitario para eliminar permisos
    public void eliminarPermiso(Permission permiso) {
        if (permisos != null) {
            permisos.remove(permiso);
        }
    }

    // Método para comprobar si el rol tiene un permiso específico
    public boolean tienePermiso(Permission permiso) {
        if (permisos == null) {
            return false;
        }
        return permisos.contains(permiso);
    }

    // Método para comprobar si el rol tiene alguno de los permisos dados
    public boolean tieneAlgunPermiso(Set<Permission> permisosRequeridos) {
        if (permisos == null || permisosRequeridos == null) {
            return false;
        }
        return permisos.stream().anyMatch(permisosRequeridos::contains);
    }
}