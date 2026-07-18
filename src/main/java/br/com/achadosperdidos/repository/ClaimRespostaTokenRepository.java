package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ClaimRespostaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimRespostaTokenRepository extends JpaRepository<ClaimRespostaToken, Long> {
    Optional<ClaimRespostaToken> findByCdTokenAndFgExcluidoFalse(String cdToken);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE ClaimRespostaToken t SET t.fgAtivo = false WHERE t.claim.id = :claimId AND t.fgExcluido = false AND t.fgAtivo = true")
    int invalidarAtivosDoClaim(@Param("claimId") Long claimId);
}
