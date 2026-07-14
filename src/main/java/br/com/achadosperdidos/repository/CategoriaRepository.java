package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    List<Categoria> findByFgExcluidoFalseOrderByOrOrdemAsc();
    // ---- Hierarquia pai/filho (coleta de itens) ----
    List<Categoria> findByCategoriaPaiIsNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    List<Categoria> findByCategoriaPaiIsNullAndFgExcluidoFalseOrderByOrOrdemAsc();
    List<Categoria> findByCategoriaPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc(Long idPai);
    boolean existsByNmCategoriaIgnoreCaseAndFgExcluidoFalse(String nmCategoria);
    boolean existsByNmCategoriaIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmCategoria, Long id);
}
