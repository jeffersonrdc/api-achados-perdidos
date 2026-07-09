package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.StatusItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StatusItemRepository extends JpaRepository<StatusItem, Long> {
    List<StatusItem> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    Optional<StatusItem> findByNmStatus(String nmStatus);
}
