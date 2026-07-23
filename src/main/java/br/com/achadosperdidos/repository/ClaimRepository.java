package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Categoria;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.StatusItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {
    long countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatusIn(Long eventoId, java.util.Collection<String> statuses);

    long countByEvento_IdAndFgExcluidoFalse(Long eventoId);

    @Query("""
            SELECT COUNT(c) FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND (:tipo IS NULL OR c.tpClaim = :tipo)
            """)
    long countByEventoAndTipo(@Param("ev") Long ev, @Param("tipo") String tipo);

    @Query("""
            SELECT COUNT(c) FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND (:tipo IS NULL OR c.tpClaim = :tipo)
              AND c.status.nmStatus IN :statuses
            """)
    long countByEventoTipoAndStatus(@Param("ev") Long ev,
                                    @Param("tipo") String tipo,
                                    @Param("statuses") java.util.Collection<String> statuses);

    @EntityGraph(attributePaths = {"evento", "categoria", "subcategoria", "status", "local"})
    Page<Claim> findByFgExcluidoFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "categoria", "subcategoria", "status", "local"})
    Page<Claim> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "categoria", "subcategoria", "status", "local"})
    java.util.List<Claim> findByNmEmailIgnoreCaseAndEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(String nmEmail, Long eventoId);

    @Query("""
            SELECT DISTINCT c.categoria FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND (:tipo IS NULL OR c.tpClaim = :tipo)
            ORDER BY c.categoria.nmCategoria
            """)
    java.util.List<Categoria> findDistinctCategorias(@Param("ev") Long ev, @Param("tipo") String tipo);

    @Query("""
            SELECT DISTINCT c.status FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND (:tipo IS NULL OR c.tpClaim = :tipo)
            ORDER BY c.status.nmStatus
            """)
    java.util.List<StatusItem> findDistinctStatus(@Param("ev") Long ev, @Param("tipo") String tipo);

    @Query("""
            SELECT DISTINCT c.nmLocal FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND (:tipo IS NULL OR c.tpClaim = :tipo)
              AND c.nmLocal IS NOT NULL AND c.nmLocal <> ''
            ORDER BY c.nmLocal
            """)
    java.util.List<String> findDistinctLocais(@Param("ev") Long ev, @Param("tipo") String tipo);

    /** Claims PERDA do evento/categoria elegíveis para o motor de match (exclui rascunho). */
    @EntityGraph(attributePaths = {"evento", "categoria", "subcategoria", "status", "local"})
    @Query("""
            SELECT c FROM Claim c
            WHERE c.evento.id = :ev AND c.fgExcluido = false
              AND c.tpClaim = :tipo
              AND c.categoria.id = :cat
              AND (c.status IS NULL OR LOWER(c.status.nmStatus) <> 'rascunho')
            ORDER BY c.dtCadastro ASC
            """)
    java.util.List<Claim> findPerdasParaMatch(@Param("ev") Long ev,
                                              @Param("tipo") String tipo,
                                              @Param("cat") Long cat);
}
