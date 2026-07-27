package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoAcaoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DevolucaoAcaoTokenRepository extends JpaRepository<DevolucaoAcaoToken, Long> {
    Optional<DevolucaoAcaoToken> findByCdTokenAndFgExcluidoFalse(String cdToken);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE DevolucaoAcaoToken t SET t.fgAtivo = false
             WHERE t.devolucao.id = :devolucaoId
               AND t.tpAcao = :tpAcao
               AND t.fgExcluido = false
               AND t.fgAtivo = true
            """)
    int invalidarAtivos(@Param("devolucaoId") Long devolucaoId, @Param("tpAcao") String tpAcao);
}
