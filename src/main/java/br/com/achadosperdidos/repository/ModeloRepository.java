package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Modelo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeloRepository extends JpaRepository<Modelo, Long> {
    List<Modelo> findByMarca_NmMarcaAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmModeloAsc(String nmMarca);

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
