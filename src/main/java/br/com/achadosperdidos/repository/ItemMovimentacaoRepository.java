package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ItemMovimentacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemMovimentacaoRepository extends JpaRepository<ItemMovimentacao, Long> {
    @EntityGraph(attributePaths = {"item", "localizacaoOrigem", "localizacaoDestino"})
    List<ItemMovimentacao> findByItem_IdAndFgExcluidoFalseOrderByDtMovimentoDesc(Long itemId);

    @EntityGraph(attributePaths = {"item", "localizacaoOrigem", "localizacaoOrigem.deposito",
            "localizacaoDestino", "localizacaoDestino.deposito"})
    List<ItemMovimentacao> findByItem_Evento_IdAndFgExcluidoFalseOrderByDtMovimentoDesc(Long eventoId);
}
