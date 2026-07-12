package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.UsuarioPermissao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioPermissaoRepository extends JpaRepository<UsuarioPermissao, Long> {
    @EntityGraph(attributePaths = {"permissao"})
    List<UsuarioPermissao> findByUsuario_IdAndFgExcluidoFalse(Long usuarioId);
    Optional<UsuarioPermissao> findByUsuario_IdAndPermissao_Id(Long usuarioId, Long permissaoId);
}
