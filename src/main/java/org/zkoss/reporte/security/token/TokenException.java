package org.zkoss.reporte.security.token;


public class TokenException extends RuntimeException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class TokenExpiredException extends TokenException {
        public TokenExpiredException() {
            super("El token ha expirado");
        }
    }

    public static class InvalidTokenException extends TokenException {
        public InvalidTokenException() {
            super("Token inválido o mal formado");
        }
    }

    public static class TokenRevokedException extends TokenException {
        public TokenRevokedException() {
            super("El token ha sido revocado");
        }
    }
}
