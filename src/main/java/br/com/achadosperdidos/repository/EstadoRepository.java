package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long> {
    List<Estado> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEstadoAsc();
    List<Estado> findByFgExcluidoFalseOrderByOrOrdemAscNmEstadoAsc();
    boolean existsByNmEstadoIgnoreCaseAndFgExcluidoFalse(String nmEstado);
    boolean existsByNmEstadoIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmEstado, Long id);
}
