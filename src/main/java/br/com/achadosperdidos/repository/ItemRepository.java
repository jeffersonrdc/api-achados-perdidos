package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByFgExcluidoFalse(Pageable pageable);
    @EntityGraph(attributePaths = {"evento", "categoria", "status"})
    Page<Item> findByEvento_IdAndFgExcluidoFalse(Long eventoId, Pageable pageable);
}
