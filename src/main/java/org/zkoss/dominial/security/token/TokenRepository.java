package org.zkoss.dominial.security.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("select t from Token t inner join Usuario u on t.usuario.id = u.id where u.id = :usuarioId and (t.expirado = false or t.revocado = false)")
    List<Token> findAllValidTokensByUsuario(Long usuarioId);

    Optional<Token> findByToken(String token);
}