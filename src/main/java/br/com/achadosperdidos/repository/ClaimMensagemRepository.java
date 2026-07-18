package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ClaimMensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClaimMensagemRepository extends JpaRepository<ClaimMensagem, Long> {
    /** Ordem cronológica da conversa (mais antigas primeiro; recentes embaixo no chat). */
    List<ClaimMensagem> findByClaim_IdAndFgExcluidoFalseOrderByIdAsc(Long claimId);

    long countByClaim_IdAndTpAutorAndFgLidaOperadorFalseAndFgExcluidoFalse(Long claimId, String tpAutor);

    @Query("""
            SELECT m.claim.id, COUNT(m)
            FROM ClaimMensagem m
            WHERE m.claim.id IN :claimIds
              AND m.tpAutor = 'SOLICITANTE'
              AND m.fgLidaOperador = false
              AND m.fgExcluido = false
            GROUP BY m.claim.id
            """)
    List<Object[]> contarNaoLidasPorClaims(@Param("claimIds") Collection<Long> claimIds);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ClaimMensagem m
            SET m.fgLidaOperador = true
            WHERE m.claim.id = :claimId
              AND m.tpAutor = 'SOLICITANTE'
              AND m.fgLidaOperador = false
              AND m.fgExcluido = false
            """)
    int marcarLidasPeloOperador(@Param("claimId") Long claimId);
}
