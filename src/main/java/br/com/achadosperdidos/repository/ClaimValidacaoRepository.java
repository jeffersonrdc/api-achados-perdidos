package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ClaimValidacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ClaimValidacaoRepository extends JpaRepository<ClaimValidacao, Long> {
    List<ClaimValidacao> findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(Long claimId);

    List<ClaimValidacao> findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(
            Long claimId, String stResultado);

    /** Candidatos em qualquer um dos resultados informados (ex.: PENDENTE + REPROVADO). */
    List<ClaimValidacao> findByClaim_IdAndStResultadoInAndFgExcluidoFalseOrderByQtSimilaridadeDesc(
            Long claimId, Collection<String> stResultados);

    long countByClaim_IdAndStResultadoAndFgExcluidoFalse(Long claimId, String stResultado);

    /** Reprovação é definitiva: usado para bloquear novo pedido do mesmo par claim↔item. */
    boolean existsByClaim_IdAndItem_IdAndStResultadoAndFgExcluidoFalse(
            Long claimId, Long itemId, String stResultado);
}
