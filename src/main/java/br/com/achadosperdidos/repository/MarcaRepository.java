package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {
    List<Marca> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmMarcaAsc();
    List<Marca> findByFgExcluidoFalseOrderByOrOrdemAscNmMarcaAsc();
    Optional<Marca> findByNmMarcaIgnoreCaseAndFgExcluidoFalse(String nmMarca);
    boolean existsByNmMarcaIgnoreCaseAndFgExcluidoFalse(String nmMarca);
    boolean existsByNmMarcaIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmMarca, Long id);
}
