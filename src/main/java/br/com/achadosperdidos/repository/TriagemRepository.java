package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Triagem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TriagemRepository extends JpaRepository<Triagem, Long> {
    @EntityGraph(attributePaths = {"item", "operador", "localizacaoInicial"})
    Optional<Triagem> findByItem_IdAndFgExcluidoFalse(Long itemId);

    /** Inclui soft-deleted — a UNIQUE em IDR_Item exige reaproveitar a linha. */
    @EntityGraph(attributePaths = {"item", "operador", "localizacaoInicial"})
    Optional<Triagem> findByItem_Id(Long itemId);
}
