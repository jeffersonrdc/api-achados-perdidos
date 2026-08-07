package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {
    List<Categoria> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    List<Categoria> findByFgExcluidoFalseOrderByOrOrdemAsc();
    // ---- Hierarquia pai/filho (selects / coleta) — ordem alfabética ----
    List<Categoria> findByCategoriaPaiIsNullAndFgExcluidoFalseAndFgAtivoTrueOrderByNmCategoriaAsc();
    List<Categoria> findByCategoriaPaiIsNullAndFgExcluidoFalseOrderByNmCategoriaAsc();
    List<Categoria> findByCategoriaPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByNmCategoriaAsc(Long idPai);
    List<Categoria> findByCategoriaPai_IdAndFgExcluidoFalseOrderByNmCategoriaAsc(Long idPai);
    List<Categoria> findByCategoriaPaiIsNotNullAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAsc();
    List<Categoria> findByCategoriaPaiIsNotNullAndFgExcluidoFalseOrderByOrOrdemAsc();
    boolean existsByNmCategoriaIgnoreCaseAndFgExcluidoFalse(String nmCategoria);
    boolean existsByNmCategoriaIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmCategoria, Long id);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndFgExcluidoFalse(String nmCategoria, Long idPai);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPai_IdAndIdNotAndFgExcluidoFalse(String nmCategoria, Long idPai, Long id);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndFgExcluidoFalse(String nmCategoria);
    boolean existsByNmCategoriaIgnoreCaseAndCategoriaPaiIsNullAndIdNotAndFgExcluidoFalse(String nmCategoria, Long id);
}
