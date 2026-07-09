package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Crianca;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriancaRepository extends JpaRepository<Crianca, Long> {
    @EntityGraph(attributePaths = {"evento"})
    List<Crianca> findByEvento_IdAndFgExcluidoFalseOrderByDtCadastroDesc(Long eventoId);
}
