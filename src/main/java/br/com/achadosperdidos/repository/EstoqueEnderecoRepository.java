package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EstoqueEndereco;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstoqueEnderecoRepository extends JpaRepository<EstoqueEndereco, Long> {
    List<EstoqueEndereco> findByDeposito_IdAndTpNivelAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(
            Long depositoId, String tpNivel);

    @EntityGraph(attributePaths = {"enderecoPai"})
    List<EstoqueEndereco> findByDeposito_IdAndTpNivelAndFgExcluidoFalseOrderByOrOrdemAscNmEnderecoAsc(
            Long depositoId, String tpNivel);

    @EntityGraph(attributePaths = {"enderecoPai"})
    List<EstoqueEndereco> findByDeposito_IdAndTpNivelAndEnderecoPai_IdAndFgExcluidoFalseOrderByOrOrdemAscNmEnderecoAsc(
            Long depositoId, String tpNivel, Long idPai);

    @EntityGraph(attributePaths = {"enderecoPai"})
    List<EstoqueEndereco> findByDeposito_IdAndTpNivelAndEnderecoPai_IdAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmEnderecoAsc(
            Long depositoId, String tpNivel, Long idPai);

    boolean existsByDeposito_IdAndTpNivelAndNmEnderecoIgnoreCaseAndFgExcluidoFalse(
            Long depositoId, String tpNivel, String nmEndereco);

    boolean existsByDeposito_IdAndTpNivelAndNmEnderecoIgnoreCaseAndIdNotAndFgExcluidoFalse(
            Long depositoId, String tpNivel, String nmEndereco, Long id);
}
