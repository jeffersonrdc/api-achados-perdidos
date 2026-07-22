package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {
    List<Arquivo> findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc(String tpEntidade, Long idEntidade);

    List<Arquivo> findByTpEntidadeAndIdEntidadeInAndFgExcluidoFalse(String tpEntidade, Collection<Long> idEntidades);

    long countByTpEntidadeAndIdEntidadeAndTpArquivoIgnoreCaseAndFgExcluidoFalse(
            String tpEntidade, Long idEntidade, String tpArquivo);
}
