package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.PortalCategoriaCapa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortalCategoriaCapaRepository extends JpaRepository<PortalCategoriaCapa, Long> {
    Optional<PortalCategoriaCapa> findByCategoria_Id(Long idCategoria);

    @Query("""
            SELECT c FROM PortalCategoriaCapa c
            JOIN FETCH c.categoria
            JOIN FETCH c.arquivo
            WHERE c.fgExcluido = false
            ORDER BY c.dtCadastro DESC
            """)
    List<PortalCategoriaCapa> findByFgExcluidoFalseOrderByDtCadastroDesc();

    boolean existsByCategoria_IdAndFgExcluidoFalse(Long idCategoria);

    boolean existsByArquivo_IdAndFgExcluidoFalse(Long idArquivo);

    @Query("""
            SELECT c FROM PortalCategoriaCapa c
            JOIN FETCH c.categoria
            JOIN FETCH c.arquivo
            WHERE c.fgExcluido = false AND c.categoria.id IN :ids
            """)
    List<PortalCategoriaCapa> findByCategoria_IdInAndFgExcluidoFalse(@Param("ids") Collection<Long> idsCategoria);
}
