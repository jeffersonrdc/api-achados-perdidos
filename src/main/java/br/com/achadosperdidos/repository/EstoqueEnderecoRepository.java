package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EstoqueEndereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstoqueEnderecoRepository extends JpaRepository<EstoqueEndereco, Long> {
    List<EstoqueEndereco> findByDeposito_IdAndTpNivelAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(
            Long depositoId, String tpNivel);
}
