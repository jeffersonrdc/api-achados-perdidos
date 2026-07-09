package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ClaimValidacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimValidacaoRepository extends JpaRepository<ClaimValidacao, Long> {
    List<ClaimValidacao> findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(Long claimId);
}
