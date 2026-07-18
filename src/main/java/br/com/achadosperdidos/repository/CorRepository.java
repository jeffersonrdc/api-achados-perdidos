package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Cor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorRepository extends JpaRepository<Cor, Long> {
    List<Cor> findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmCorAsc();
    List<Cor> findByFgExcluidoFalseOrderByOrOrdemAscNmCorAsc();
    boolean existsByNmCorIgnoreCaseAndFgExcluidoFalse(String nmCor);
    boolean existsByNmCorIgnoreCaseAndIdNotAndFgExcluidoFalse(String nmCor, Long id);
}
