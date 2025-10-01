package org.zkoss.dominial.security.refresh_token;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.zkoss.dominial.security.user.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.usuario.id = :usuarioId")
    void deleteByUsuarioId(Long usuarioId);

    List<RefreshToken> findByUsuarioAndRevocadoFalse(Usuario usuario);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.usuario.id = :usuarioId AND rt.revocado = false")
    List<RefreshToken> findActiveTokensByUsuarioId(Long usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revocado = true WHERE rt.usuario.id = :usuarioId")
    void revokeAllByUsuarioId(Long usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    void deleteAllExpiredTokens();
}