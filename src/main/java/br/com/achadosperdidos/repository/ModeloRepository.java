package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Modelo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeloRepository extends JpaRepository<Modelo, Long>, JpaSpecificationExecutor<Modelo> {
    /** Select: ordem alfabética por nome (todos os modelos da marca). */
    List<Modelo> findByMarca_NmMarcaAndFgExcluidoFalseAndFgAtivoTrueOrderByNmModeloAsc(String nmMarca);

    /**
     * Select filtrado por subcategoria: modelos genéricos (sem vínculo) + da subcategoria.
     */
    @Query("""
            SELECT m FROM Modelo m
            WHERE m.marca.nmMarca = :nmMarca
              AND m.fgExcluido = false AND m.fgAtivo = true
              AND (m.subcategoria IS NULL
                   OR LOWER(m.subcategoria.nmCategoria) = LOWER(:nmSubcategoria))
            ORDER BY m.nmModelo ASC
            """)
    List<Modelo> findAtivosByMarcaAndSubcategoria(@Param("nmMarca") String nmMarca,
                                                  @Param("nmSubcategoria") String nmSubcategoria);

    @EntityGraph(attributePaths = {"marca"})
    List<Modelo> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmModeloAsc();

    @EntityGraph(attributePaths = {"marca"})
    List<Modelo> findByFgExcluidoFalseOrderByOrOrdemAscNmModeloAsc();

    @EntityGraph(attributePaths = {"marca"})
    List<Modelo> findByMarca_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmModeloAsc(Long idMarca);

    @EntityGraph(attributePaths = {"marca"})
    List<Modelo> findByMarca_IdAndFgExcluidoFalseOrderByOrOrdemAscNmModeloAsc(Long idMarca);

    boolean existsByNmModeloIgnoreCaseAndMarca_IdAndFgExcluidoFalse(String nmModelo, Long idMarca);
    boolean existsByNmModeloIgnoreCaseAndMarca_IdAndIdNotAndFgExcluidoFalse(String nmModelo, Long idMarca, Long id);
}
