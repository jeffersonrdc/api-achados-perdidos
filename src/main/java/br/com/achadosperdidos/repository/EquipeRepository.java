package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Equipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long>, JpaSpecificationExecutor<Equipe> {
    @EntityGraph(attributePaths = {"evento", "local"})
    List<Equipe> findByEvento_IdAndFgExcluidoFalseOrderByNmEquipeAsc(Long eventoId);
}
