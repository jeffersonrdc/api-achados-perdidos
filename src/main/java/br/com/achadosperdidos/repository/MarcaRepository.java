package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long>, JpaSpecificationExecutor<Marca> {
    /** Select: ordem alfabética por nome. */
    List<Marca> findByFgExcluidoFalseAndFgAtivoTrueOrderByNmMarcaAsc();
    List<Marca> findByFgExcluidoFalseOrderByOrOrdemAscNmMarcaAsc();
    Optional<Marca> findByNmMarcaIgnoreCaseAndFgExcluidoFalse(String nmMarca);
    boolean existsByNmMarcaIgnoreCaseAndFgExcluidoFalse(String nmMarca);
    boolean existsByNmMarcaIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmMarca, Long id);

    /**
     * Marcas da subcategoria: vinculadas em marca_subcategoria OU sem vínculo (genéricas).
     * Espelho do filtro de tags por subcategoria, permitindo marca em N subs.
     */
    @Query("""
            SELECT DISTINCT m FROM Marca m
            LEFT JOIN m.subcategorias s
            WHERE m.fgExcluido = false AND m.fgAtivo = true
              AND (
                    SIZE(m.subcategorias) = 0
                 OR LOWER(s.nmCategoria) = LOWER(:nmSubcategoria)
              )
            ORDER BY m.nmMarca ASC
            """)
    List<Marca> findAtivasBySubcategoria(@Param("nmSubcategoria") String nmSubcategoria);
}
