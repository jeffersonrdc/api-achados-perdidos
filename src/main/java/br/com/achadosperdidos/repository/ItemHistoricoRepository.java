package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ItemHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemHistoricoRepository extends JpaRepository<ItemHistorico, Long> {
    List<ItemHistorico> findByItem_IdAndFgExcluidoFalseOrderByDtHistoricoDesc(Long itemId);
}
