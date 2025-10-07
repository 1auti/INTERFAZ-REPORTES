package org.zkoss.reporte.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.zkoss.reporte.security.config.JwtProperties;
import org.zkoss.reporte.security.user.Usuario;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtProperties jwtProperties;

    public void guardarToken(Usuario usuario, String jwtToken) {
        Date fechaExpiracion = calcularFechaExpiracion();

        Token token = Token.builder()
                .usuario(usuario)
                .token(jwtToken)
                .tipoToken(TokenType.BEARER)
                .expirado(false)
                .revocado(false)
                .fechaExpiracion(fechaExpiracion)
                .build();

        tokenRepository.save(token);
    }

    public void revocarTodosLosTokens(Usuario usuario) {
        List<Token> tokens = tokenRepository.findAllValidTokensByUsuario(usuario.getId());
        if (tokens.isEmpty()) {
            return;
        }

        tokens.forEach(token -> {
            token.setRevocado(true);
            token.setExpirado(true);
        });

        tokenRepository.saveAll(tokens);
    }

    public Optional<Token> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void marcarTokenComoExpirado(Token token) {
        token.setExpirado(true);
        tokenRepository.save(token);
    }

    public void marcarTokenComoRevocado(Token token) {
        token.setRevocado(true);
        tokenRepository.save(token);
    }

//    private Date calcularFechaExpiracion() {
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.MILLISECOND, (int) jwtProperties.getExpiration());
//        return calendar.getTime();
//    }

    private Date calcularFechaExpiracion() {
        // En lugar de usar la expiración configurada, establecer una fecha muy lejana
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 100); // Fecha de expiración en 100 años
        return calendar.getTime();
    }
}