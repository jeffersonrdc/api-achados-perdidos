package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Localizacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {
    @EntityGraph(attributePaths = {"deposito"})
    List<Localizacao> findByDeposito_IdAndFgExcluidoFalseOrderByIdAsc(Long depositoId);
}
