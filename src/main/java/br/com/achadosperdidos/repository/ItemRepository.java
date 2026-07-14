package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    List<Item> findByEvento_IdAndStatus_NmStatusInAndFgExcluidoFalseOrderByDtCadastroAsc(
            Long eventoId, Collection<String> statuses);

    @EntityGraph(attributePaths = {"categoria", "subcategoria", "status", "localizacao", "localizacao.deposito"})
    List<Item> findByEvento_IdAndStatus_NmStatusAndFgExcluidoFalseOrderByDtEncontradoDesc(
            Long eventoId, String status);

    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByFgExcluidoFalse(Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueAndFgEntregueFalseAndFgDescartadoFalse(
            Long eventoId, Pageable pageable);

    // ---- Coleta: resumo/KPIs ----
    long countByEvento_IdAndFgExcluidoFalse(Long eventoId);
    long countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatus(Long eventoId, String nmStatus);
    long countByEvento_IdAndFgExcluidoFalseAndTpPrioridade(Long eventoId, String tpPrioridade);
    long countByEvento_IdAndFgExcluidoFalseAndFgSensivelTrue(Long eventoId);

    // ---- Coleta: filtros (locais distintos com item cadastrado no evento) ----
    @Query("""
            SELECT DISTINCT i.nmLocalEncontrado FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false
               AND i.nmLocalEncontrado IS NOT NULL AND i.nmLocalEncontrado <> ''
             ORDER BY i.nmLocalEncontrado""")
    List<String> findDistinctLocais(@Param("eventoId") Long eventoId);

    // ---- Triagem: resumo/KPIs ----
    long countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatusIn(Long eventoId, Collection<String> statuses);
    long countByEvento_IdAndFgExcluidoFalseAndFgSensivelTrueAndStatus_NmStatusIn(Long eventoId, Collection<String> statuses);

    @Query("""
            SELECT COUNT(DISTINCT i.categoria.id) FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false
               AND i.status.nmStatus IN :statuses""")
    long countCategoriasDistintas(@Param("eventoId") Long eventoId, @Param("statuses") Collection<String> statuses);
}
