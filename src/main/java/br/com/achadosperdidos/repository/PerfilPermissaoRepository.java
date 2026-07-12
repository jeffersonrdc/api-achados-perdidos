package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.PerfilPermissao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilPermissaoRepository extends JpaRepository<PerfilPermissao, Long> {
    @EntityGraph(attributePaths = {"permissao"})
    List<PerfilPermissao> findByPerfil_IdAndFgExcluidoFalse(Long perfilId);
    Optional<PerfilPermissao> findByPerfil_IdAndPermissao_Id(Long perfilId, Long permissaoId);
}
