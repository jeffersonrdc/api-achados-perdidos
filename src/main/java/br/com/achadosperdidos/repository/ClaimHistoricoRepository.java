package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ClaimHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimHistoricoRepository extends JpaRepository<ClaimHistorico, Long> {
    List<ClaimHistorico> findByClaim_IdAndFgExcluidoFalseOrderByDtHistoricoDesc(Long claimId);

    long countByItem_IdAndTpEventoAndFgExcluidoFalse(Long itemId, String tpEvento);

    /** Quantidade de claims distintos que já referenciaram o item (pedidos por item). */
    @Query("select count(distinct h.claim.id) from ClaimHistorico h where h.item.id = :itemId and h.fgExcluido = false")
    long countPedidosDistintosPorItem(@Param("itemId") Long itemId);
}
