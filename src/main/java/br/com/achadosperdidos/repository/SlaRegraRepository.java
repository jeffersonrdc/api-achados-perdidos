package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.SlaRegra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SlaRegraRepository extends JpaRepository<SlaRegra, Long> {
    List<SlaRegra> findByFgExcluidoFalseAndFgAtivoTrueOrderByTpProcessoAsc();
    List<SlaRegra> findByEvento_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByTpProcessoAsc(Long eventoId);
}
