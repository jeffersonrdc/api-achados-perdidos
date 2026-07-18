package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Devolucao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucaoRepository extends JpaRepository<Devolucao, Long>, JpaSpecificationExecutor<Devolucao> {
    @EntityGraph(attributePaths = {"item", "item.categoria", "claim"})
    List<Devolucao> findByFgExcluidoFalseOrderByDtDevolucaoDesc();

    @EntityGraph(attributePaths = {"item", "item.categoria", "claim"})
    List<Devolucao> findByEvento_IdAndFgExcluidoFalseOrderByDtDevolucaoDesc(Long eventoId);

    @EntityGraph(attributePaths = {"item", "item.categoria", "claim"})
    Page<Devolucao> findByFgExcluidoFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.categoria", "claim"})
    Page<Devolucao> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);

    long countByEvento_IdAndFgExcluidoFalse(Long eventoId);
    long countByEvento_IdAndFgExcluidoFalseAndTpStatus(Long eventoId, String tpStatus);

    /** Locais (do item) presentes nas devoluções do evento. */
    @Query("""
            SELECT DISTINCT d.item.nmLocalEncontrado FROM Devolucao d
            WHERE d.evento.id = :ev AND d.fgExcluido = false
              AND d.item.nmLocalEncontrado IS NOT NULL AND d.item.nmLocalEncontrado <> ''
            ORDER BY d.item.nmLocalEncontrado
            """)
    List<String> findDistinctLocais(@Param("ev") Long ev);

    /** Prioridades (do item) presentes nas devoluções do evento. */
    @Query("""
            SELECT DISTINCT d.item.tpPrioridade FROM Devolucao d
            WHERE d.evento.id = :ev AND d.fgExcluido = false
              AND d.item.tpPrioridade IS NOT NULL AND d.item.tpPrioridade <> ''
            ORDER BY d.item.tpPrioridade
            """)
    List<String> findDistinctPrioridades(@Param("ev") Long ev);
}
