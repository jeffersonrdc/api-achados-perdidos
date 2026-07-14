package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    /** Solicitações (claims) pendentes de um evento por lista de status. */
    long countByEvento_IdAndFgExcluidoFalseAndStatus_NmStatusIn(Long eventoId, java.util.Collection<String> statuses);

    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Claim> findByFgExcluidoFalse(Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Claim> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);

    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    java.util.List<Claim> findByNmEmailIgnoreCaseAndEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(String nmEmail, Long eventoId);
}
