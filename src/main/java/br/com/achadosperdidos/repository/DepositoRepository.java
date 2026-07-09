package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Deposito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositoRepository extends JpaRepository<Deposito, Long> {
    @EntityGraph(attributePaths = {"evento"})
    List<Deposito> findByEvento_IdAndFgExcluidoFalseOrderByNmDepositoAsc(Long eventoId);
}
