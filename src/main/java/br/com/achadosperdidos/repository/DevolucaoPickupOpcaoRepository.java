package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.DevolucaoPickupOpcao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DevolucaoPickupOpcaoRepository extends JpaRepository<DevolucaoPickupOpcao, Long> {
    List<DevolucaoPickupOpcao> findByDevolucao_IdAndFgExcluidoFalseOrderByDtOpcaoAscHrInicioAsc(Long devolucaoId);

    Optional<DevolucaoPickupOpcao> findByIdAndDevolucao_IdAndFgExcluidoFalse(Long id, Long devolucaoId);
}
