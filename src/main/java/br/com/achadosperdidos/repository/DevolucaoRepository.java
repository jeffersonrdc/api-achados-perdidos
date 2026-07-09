package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Devolucao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {
    @EntityGraph(attributePaths = {"item", "claim"})
    List<Devolucao> findByFgExcluidoFalseOrderByDtDevolucaoDesc();
}
