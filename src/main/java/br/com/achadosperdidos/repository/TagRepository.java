package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Tag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmTagAsc();

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findByFgExcluidoFalseOrderByOrOrdemAscNmTagAsc();

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findBySubcategoria_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmTagAsc(Long idSubcategoria);

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findBySubcategoria_IdAndFgExcluidoFalseOrderByOrOrdemAscNmTagAsc(Long idSubcategoria);

    boolean existsByNmTagIgnoreCaseAndSubcategoria_IdAndFgExcluidoFalse(String nmTag, Long idSubcategoria);
    boolean existsByNmTagIgnoreCaseAndSubcategoria_IdAndIdNotAndFgExcluidoFalse(String nmTag, Long idSubcategoria, Long id);
}
