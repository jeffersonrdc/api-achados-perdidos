package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByFgExcluidoFalseOrderByDtAuditoriaDesc(Pageable pageable);

    Page<Auditoria> findByNmTabelaAndIdRegistroAndFgExcluidoFalseOrderByDtAuditoriaDesc(
            String nmTabela, Long idRegistro, Pageable pageable);

    @Query("""
            SELECT a FROM Auditoria a
            WHERE a.fgExcluido = false
              AND (:nmTabela IS NULL OR :nmTabela = '' OR a.nmTabela = :nmTabela)
              AND (:tpAcao IS NULL OR :tpAcao = '' OR a.tpAcao = :tpAcao)
              AND (:idUsuario IS NULL OR a.idUsuario = :idUsuario)
              AND (:nrIp IS NULL OR :nrIp = '' OR a.nrIp = :nrIp)
              AND (:de IS NULL OR a.dtAuditoria >= :de)
              AND (:ate IS NULL OR a.dtAuditoria <= :ate)
            ORDER BY a.dtAuditoria DESC
            """)
    Page<Auditoria> buscarFiltrado(@Param("nmTabela") String nmTabela,
                                   @Param("tpAcao") String tpAcao,
                                   @Param("idUsuario") Long idUsuario,
                                   @Param("nrIp") String nrIp,
                                   @Param("de") LocalDateTime de,
                                   @Param("ate") LocalDateTime ate,
                                   Pageable pageable);
}
