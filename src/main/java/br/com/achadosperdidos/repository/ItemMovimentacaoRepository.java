package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ItemMovimentacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemMovimentacaoRepository extends JpaRepository<ItemMovimentacao, Long>, JpaSpecificationExecutor<ItemMovimentacao> {
    @EntityGraph(attributePaths = {"item", "localizacaoOrigem", "localizacaoDestino"})
    List<ItemMovimentacao> findByItem_IdAndFgExcluidoFalseOrderByDtMovimentoDesc(Long itemId);

    @EntityGraph(attributePaths = {"item", "localizacaoOrigem", "localizacaoOrigem.deposito",
            "localizacaoDestino", "localizacaoDestino.deposito"})
    List<ItemMovimentacao> findByItem_Evento_IdAndFgExcluidoFalseOrderByDtMovimentoDesc(Long eventoId);

    long countByItem_Evento_IdAndFgExcluidoFalseAndTpMovimento(Long eventoId, String tpMovimento);

    @Query("""
            SELECT m.tpMovimento AS nome, COUNT(m) AS qt FROM ItemMovimentacao m
             WHERE m.item.evento.id = :eventoId AND m.fgExcluido = false
               AND (:inicio IS NULL OR (m.dtMovimento >= :inicio AND m.dtMovimento < :fim))
             GROUP BY m.tpMovimento ORDER BY qt DESC""")
    List<Object[]> contagemPorTipo(@Param("eventoId") Long eventoId,
                                   @Param("inicio") java.time.LocalDateTime inicio,
                                   @Param("fim") java.time.LocalDateTime fim);
}
