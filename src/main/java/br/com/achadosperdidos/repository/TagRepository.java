package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Tag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {
    /** Select: ordem alfabética por nome. */
    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findByFgExcluidoFalseAndFgAtivoTrueOrderByNmTagAsc();

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findByFgExcluidoFalseOrderByNmTagAsc();

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findBySubcategoria_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByNmTagAsc(Long idSubcategoria);

    @EntityGraph(attributePaths = {"subcategoria", "subcategoria.categoriaPai"})
    List<Tag> findBySubcategoria_IdAndFgExcluidoFalseOrderByNmTagAsc(Long idSubcategoria);

    boolean existsByNmTagIgnoreCaseAndSubcategoria_IdAndFgExcluidoFalse(String nmTag, Long idSubcategoria);
    boolean existsByNmTagIgnoreCaseAndSubcategoria_IdAndIdNotAndFgExcluidoFalse(String nmTag, Long idSubcategoria, Long id);
}
