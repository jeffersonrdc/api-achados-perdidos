package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long>, JpaSpecificationExecutor<Estado> {
    /** Select: ordem alfabética por nome. */
    List<Estado> findByFgExcluidoFalseAndFgAtivoTrueOrderByNmEstadoAsc();
    List<Estado> findByFgExcluidoFalseOrderByOrOrdemAscNmEstadoAsc();
    boolean existsByNmEstadoIgnoreCaseAndFgExcluidoFalse(String nmEstado);
    boolean existsByNmEstadoIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmEstado, Long id);
}
