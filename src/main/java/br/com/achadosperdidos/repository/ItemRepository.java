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

    /** Itens públicos no portal: apenas os que já chegaram ao estoque (não entregues/descartados). */
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueAndFgEntregueFalseAndFgDescartadoFalseAndStatus_NmStatusIn(
            Long eventoId, Collection<String> statuses, Pageable pageable);

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
               AND (
                 i.status.nmStatus IN :statuses
                 OR (
                   i.status.nmStatus = 'Em estoque'
                   AND NOT EXISTS (
                     SELECT 1 FROM Triagem t
                      WHERE t.item.id = i.id AND t.fgExcluido = false AND t.tpStatus = 'CONCLUIDA'
                   )
                 )
               )""")
    long countCategoriasDistintas(@Param("eventoId") Long eventoId, @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT i.categoria.nmCategoria AS nome, COUNT(i) AS qt FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false
               AND (
                 i.status.nmStatus IN :statuses
                 OR (
                   i.status.nmStatus = 'Em estoque'
                   AND NOT EXISTS (
                     SELECT 1 FROM Triagem t
                      WHERE t.item.id = i.id AND t.fgExcluido = false AND t.tpStatus = 'CONCLUIDA'
                   )
                 )
               )
             GROUP BY i.categoria.nmCategoria ORDER BY qt DESC""")
    List<Object[]> contagemPorCategoria(@Param("eventoId") Long eventoId, @Param("statuses") Collection<String> statuses);

    /** Itens já no estoque que ainda não tiveram a triagem concluída (aparecem na fila). */
    @Query("""
            SELECT COUNT(i) FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false
               AND i.status.nmStatus = 'Em estoque'
               AND NOT EXISTS (
                 SELECT 1 FROM Triagem t
                  WHERE t.item.id = i.id AND t.fgExcluido = false AND t.tpStatus = 'CONCLUIDA'
               )""")
    long countEstoquePendenteTriagem(@Param("eventoId") Long eventoId);

    @Query("""
            SELECT COUNT(i) FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false AND i.fgSensivel = true
               AND (
                 i.status.nmStatus IN :statuses
                 OR (
                   i.status.nmStatus = 'Em estoque'
                   AND NOT EXISTS (
                     SELECT 1 FROM Triagem t
                      WHERE t.item.id = i.id AND t.fgExcluido = false AND t.tpStatus = 'CONCLUIDA'
                   )
                 )
               )""")
    long countSensiveisNaFila(@Param("eventoId") Long eventoId, @Param("statuses") Collection<String> statuses);

    @Query("""
            SELECT COUNT(i) FROM Item i
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false
               AND (
                 i.status.nmStatus IN :statuses
                 OR (
                   i.status.nmStatus = 'Em estoque'
                   AND NOT EXISTS (
                     SELECT 1 FROM Triagem t
                      WHERE t.item.id = i.id AND t.fgExcluido = false AND t.tpStatus = 'CONCLUIDA'
                   )
                 )
               )""")
    long countNaFilaTriagem(@Param("eventoId") Long eventoId, @Param("statuses") Collection<String> statuses);

    // ---- Transferência: itens disponíveis em um local ----
    @EntityGraph(attributePaths = {"categoria", "localAtual"})
    List<Item> findByEvento_IdAndLocalAtual_IdAndFgExcluidoFalseAndFgEntregueFalseAndFgDescartadoFalseOrderByNmTituloAsc(
            Long eventoId, Long localId);

    @EntityGraph(attributePaths = {"categoria", "localAtual"})
    List<Item> findByEvento_IdAndFgExcluidoFalseAndFgEntregueFalseAndFgDescartadoFalseAndLocalAtualIsNotNullOrderByNmTituloAsc(
            Long eventoId);

    // ---- Estoque: resumo (distribuição por depósito; inclui itens sem localização) ----
    @Query("""
            SELECT COALESCE(d.nmDeposito, 'Sem localização') AS nome, COUNT(i) AS qt
              FROM Item i LEFT JOIN i.localizacao l LEFT JOIN l.deposito d
             WHERE i.evento.id = :eventoId AND i.fgExcluido = false AND i.status.nmStatus = :status
             GROUP BY d.nmDeposito ORDER BY qt DESC""")
    List<Object[]> contagemEstoquePorDeposito(@Param("eventoId") Long eventoId, @Param("status") String status);
}
