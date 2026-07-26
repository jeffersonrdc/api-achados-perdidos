package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Local;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalRepository extends JpaRepository<Local, Long>, JpaSpecificationExecutor<Local> {
    @EntityGraph(attributePaths = {"evento", "responsavel"})
    List<Local> findByEvento_IdAndFgExcluidoFalseOrderByNmLocalAsc(Long eventoId);

    Optional<Local> findFirstByEvento_IdAndNmLocalIgnoreCaseAndFgExcluidoFalse(Long eventoId, String nmLocal);
    Optional<Local> findFirstByEvento_IdAndTpLocalAndFgExcluidoFalse(Long eventoId, String tpLocal);
}
