package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EtiquetaImpressao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtiquetaImpressaoRepository extends JpaRepository<EtiquetaImpressao, Long> {
    @EntityGraph(attributePaths = {"operador"})
    List<EtiquetaImpressao> findByItem_IdAndFgExcluidoFalseOrderByDtImpressaoDesc(Long itemId);
}
