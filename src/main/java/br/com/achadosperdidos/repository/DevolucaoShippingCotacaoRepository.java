package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoShippingCotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevolucaoShippingCotacaoRepository extends JpaRepository<DevolucaoShippingCotacao, Long> {
    Optional<DevolucaoShippingCotacao> findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtInformadaDesc(Long devolucaoId);
}
