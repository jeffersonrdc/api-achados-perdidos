package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissaoRepository extends JpaRepository<Permissao, Long> {

    List<Permissao> findByFgExcluidoFalseAndFgAtivoTrueOrderByNmModuloAscNmAcaoAsc();

    Optional<Permissao> findByNmPermissao(String nmPermissao);

    /**
     * Permissoes efetivas do usuario = (permissoes do perfil) UNIAO
     * (permissoes adicionais do usuario). Retorna os nomes (modulo.acao).
     */
    @Query(value = """
            SELECT DISTINCT pe.NM_Permissao
            FROM permissao pe
            WHERE pe.FG_Excluido = 0 AND pe.FG_Ativo = 1 AND (
              pe.ID_Permissao IN (
                SELECT pp.IDR_Permissao FROM perfil_permissao pp
                JOIN usuario u ON u.IDR_Perfil = pp.IDR_Perfil
                WHERE u.ID_Usuario = :usuarioId AND pp.FG_Excluido = 0 AND pp.FG_Ativo = 1
              )
              OR pe.ID_Permissao IN (
                SELECT up.IDR_Permissao FROM usuario_permissao up
                WHERE up.IDR_Usuario = :usuarioId AND up.FG_Excluido = 0 AND up.FG_Ativo = 1
              )
            )
            """, nativeQuery = true)
    List<String> findPermissoesEfetivas(@Param("usuarioId") Long usuarioId);
}
