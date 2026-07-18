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
    List<Categoria> findByCategoriaPai_IdAndFgExcluidoFalseOrderByOrOrdemAsc(Long idPai);
    List<Categoria> findByCategoriaPaiIsNotNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    List<Categoria> findByCategoriaPaiIsNotNullAndFgExcluidoFalseOrderByOrOrdemAsc();
    boolean existsByNmCategoriaIgnoreCaseAndFgExcluidoFalse(String nmCategoria);
    boolean existsByNmCategoriaIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmCategoria, Long id);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndFgExcluidoFalse(String nmCategoria, Long idPai);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndIdNotAndFgExcluidoFalse(String nmCategoria, Long idPai, Long id);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndFgExcluidoFalse(String nmCategoria);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndIdNotAndFgExcluidoFalse(String nmCategoria, Long id);
}
