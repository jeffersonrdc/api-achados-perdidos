package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.ItemCampo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ItemCampoRepository extends JpaRepository<ItemCampo, Long> {
    List<ItemCampo> findByItem_IdAndFgExcluidoFalseOrderByIdAsc(Long itemId);
    Optional<ItemCampo> findByItem_IdAndCategoriaCampo_IdAndFgExcluidoFalse(Long itemId, Long categoriaCampoId);
}
