package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Local;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalRepository extends JpaRepository<Local, Long> {
    @EntityGraph(attributePaths = {"evento", "responsavel"})
    List<Local> findByEvento_IdAndFgExcluidoFalseOrderByNmLocalAsc(Long eventoId);
}
