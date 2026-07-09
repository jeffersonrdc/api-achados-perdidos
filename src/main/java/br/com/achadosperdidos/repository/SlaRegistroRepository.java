package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.SlaRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaRegistroRepository extends JpaRepository<SlaRegistro, Long> {
    List<SlaRegistro> findByStSlaInAndFgExcluidoFalseOrderByDtLimiteAsc(List<String> status);
}
