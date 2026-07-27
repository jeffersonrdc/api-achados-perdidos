package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucaoHistoricoRepository extends JpaRepository<DevolucaoHistorico, Long> {
    List<DevolucaoHistorico> findByDevolucao_IdAndFgExcluidoFalseOrderByDtEventoDesc(Long devolucaoId);
}
