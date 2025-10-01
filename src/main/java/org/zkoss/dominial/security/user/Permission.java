package org.zkoss.dominial.security.user;

public enum Permission {
    // Permisos generales
    READ("read"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),

    // Permisos específicos por módulo
    PROYECTOS_READ("proyectos:read"),
    PROYECTOS_CREATE("proyectos:create"),
    PROYECTOS_UPDATE("proyectos:update"),
    PROYECTOS_DELETE("proyectos:delete"),

    DATOS_READ("datos:read"),
    DATOS_CREATE("datos:create"),
    DATOS_UPDATE("datos:update"),
    DATOS_DELETE("datos:delete"),

    // Más permisos específicos...

    ADMIN_ALL("admin:all");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}